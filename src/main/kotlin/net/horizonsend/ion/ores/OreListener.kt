package net.horizonsend.ion.ores

import kotlin.random.Random
import net.horizonsend.ion.Ion
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.data.BlockData
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.persistence.PersistentDataType

internal class OreListener(private val plugin: Ion) : Listener {
	init {
		plugin.server.pluginManager.registerEvents(this, plugin)
	}

	private val currentOreVersion = 2

	private val oreCheckNamespace = NamespacedKey(plugin, "oreCheck")

	private class BlockLocation(var x: Int, var y: Int, var z: Int)

	@EventHandler
	fun onChunkLoad(event: ChunkLoadEvent) {
		if (event.chunk.persistentDataContainer.get(oreCheckNamespace, PersistentDataType.INTEGER) == currentOreVersion)
			return

		Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
			val chunkSnapshot = event.chunk.getChunkSnapshot(false, false, false)
			val placementConfiguration =
				OrePlacementConfig.values().find { it.name == chunkSnapshot.worldName } ?: return@Runnable
			val random = Random(event.chunk.chunkKey)

			// These are kept separate as ores need to be written to a file,
			// reversing ores does not need to be written to a file.
			val placedBlocks = mutableMapOf<BlockLocation, BlockData>() // Everything
			val placedOres = mutableMapOf<BlockLocation, Ore>() // Everything that needs to be written to a file.

			val file =
				plugin.dataFolder.resolve("ores/${chunkSnapshot.worldName}/${chunkSnapshot.x}_${chunkSnapshot.z}.ores.csv")

			if (file.exists()) file.readText().split("\n").forEach { oreLine ->
				val oreData = oreLine.split(",")

				if (oreData.size != 5)
					throw IllegalArgumentException("${file.absolutePath} ore data line $oreLine is not valid.")

				val x = oreData[0].toInt()
				val y = oreData[1].toInt()
				val z = oreData[2].toInt()
				val original = Material.valueOf(oreData[3])
				val placedOre = Ore.valueOf(oreData[4])

				if (chunkSnapshot.getBlockData(x, y, z) == placedOre.blockData)
					placedBlocks[BlockLocation(x, y, z)] = original.createBlockData()
			}

			for (sectionY in event.chunk.world.minHeight.shr(16)..event.chunk.world.maxHeight.shr(16)) {
				if (chunkSnapshot.isSectionEmpty(sectionY)) continue

				for (x in 0..15) for (y in 0..15) for (z in 0..15) {
					val blockData = chunkSnapshot.getBlockData(x, y + sectionY.shl(16), z)

					if (blockData.material.isAir) continue
					if (blockData.material.isInteractable) continue

					if (x < 15) if (chunkSnapshot.getBlockType(x + 1, y, z).isAir) continue
					if (y < 15) if (chunkSnapshot.getBlockType(x, y + 1, z).isAir) continue
					if (z < 15) if (chunkSnapshot.getBlockType(x, y, z + 1).isAir) continue

					if (x > 0) if (chunkSnapshot.getBlockType(x - 1, y, z).isAir) continue
					if (y > 0) if (chunkSnapshot.getBlockType(x, y - 1, z).isAir) continue
					if (z > 0) if (chunkSnapshot.getBlockType(x, y, z - 1).isAir) continue

					placementConfiguration.options.forEach { (ore, chance) ->
						if (random.nextDouble(0.0, 100.0) > 0.3 * (1 / chance)) return@forEach
						placedOres[BlockLocation(x, y, z)] = ore
					}
				}
			}

			placedBlocks.putAll(placedOres.map { Pair(it.key, it.value.blockData) })

			Bukkit.getScheduler().runTask(plugin, Runnable {
				placedBlocks.forEach { (position, blockData) ->
					event.chunk.getBlock(position.x, position.y, position.z).setBlockData(blockData, false)
				}

				event.chunk.persistentDataContainer.set(oreCheckNamespace, PersistentDataType.INTEGER, currentOreVersion)
			})

			plugin.dataFolder.resolve("ores/${chunkSnapshot.worldName}")
				.apply { mkdirs() }
				.resolve("${chunkSnapshot.x}_${chunkSnapshot.z}.ores.csv")
				.writeText(placedOres.map {
					"${it.key.x},${it.key.y},${it.key.z},${chunkSnapshot.getBlockType(it.key.x, it.key.y, it.key.z)},${it.value}"
				}.joinToString("\n", "", ""))
		})
	}
}