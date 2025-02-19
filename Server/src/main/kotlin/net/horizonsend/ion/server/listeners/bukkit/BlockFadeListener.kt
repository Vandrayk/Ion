package net.horizonsend.ion.server.listeners.bukkit

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFadeEvent

@Suppress("Unused")
class BlockFadeListener : Listener {
	@EventHandler(priority = EventPriority.LOW)
	fun onBlockFadeEvent(event: BlockFadeEvent) {
		if (event.block.type != Material.ICE) return

		event.isCancelled = true
	}
}