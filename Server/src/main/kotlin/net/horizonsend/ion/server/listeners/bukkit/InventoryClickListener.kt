package net.horizonsend.ion.server.listeners.bukkit

import net.horizonsend.ion.server.managers.ScreenManager.isInScreen
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

@Suppress("Unused")
class InventoryClickListener : Listener {
	@EventHandler(priority = EventPriority.LOW)
	fun onInventoryClickEvent(event: InventoryClickEvent) {
		if (event.whoClicked.isInScreen) event.isCancelled = true
	}
}