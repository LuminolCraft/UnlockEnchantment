package org.luminolcraft.unlockEnchantment

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.ItemStack

class CraftListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPrepareCraft(event: PrepareItemCraftEvent) {
        if (event.inventory.matrix.filterNotNull()
                .filter { !it.isEmpty }.size != 2 || !Main.configManager.isEnchantmentSimplify
        ) return
        if (event.inventory.matrix.firstOrNull { it?.type == Material.ENCHANTED_BOOK } == null ||
            event.inventory.matrix.firstOrNull { it?.type == Material.BOOK } == null) return
        val matrix: List<ItemStack> = event.inventory.matrix.filterNotNull().filter { !it.isEmpty }
        val craftEnchantedBook: ItemStack = matrix.first { it.type == Material.ENCHANTED_BOOK }
        val resultEnchantedBook: ItemStack = craftEnchantedBook.clone()
        event.inventory.result = resultEnchantedBook
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onCraft(event: InventoryClickEvent) {
        if (!Main.configManager.isEnchantmentSimplify || event.inventory !is CraftingInventory || event.whoClicked !is Player) return
        if (event.slot != 0) return
        event.isCancelled = true
        val originMatrix: Array<out ItemStack?> = (event.inventory as CraftingInventory).matrix
        if (originMatrix.firstOrNull { it?.type == Material.ENCHANTED_BOOK } == null ||
            originMatrix.firstOrNull { it?.type == Material.BOOK } == null) return
        val normalBook: ItemStack = originMatrix.filterNotNull().first { it.type == Material.BOOK }
        val enchantedBook: ItemStack = originMatrix.filterNotNull().first { it.type == Material.ENCHANTED_BOOK }
        (event.whoClicked as Player).scheduler.run(Main.configManager.javaPlugin, {
            if (normalBook.amount > 0) {
                val putBook: ItemStack = normalBook.clone()
                putBook.amount -= 1
                event.inventory.setItem(originMatrix.indexOf(normalBook) + 1, putBook)
            }
            val putEnchantedBook: ItemStack = enchantedBook.clone()
            event.inventory.setItem(originMatrix.indexOf(enchantedBook) + 1, putEnchantedBook)
        }, null)
        event.view.setCursor(enchantedBook.clone())
    }
//    @EventHandler(priority = EventPriority.MONITOR)
//    fun onCraft(event: CraftItemEvent) {
//        println("CraftItem handled")
//        if (event.inventory.matrix.filterNotNull()
//                .filter { !it.isEmpty }.size != 2 || !Main.configManager.isEnchantmentSimplify
//        ) return
//        println("CraftItem Stage 1")
//        val originMatrix: Array<out ItemStack?> = event.inventory.matrix
//        if(event.inventory.matrix.firstOrNull { it?.type == Material.ENCHANTED_BOOK } == null ||
//            event.inventory.matrix.firstOrNull { it?.type == Material.BOOK } == null) return
//        println("CraftItem Stage 2")
//        val normalBook: ItemStack = originMatrix.filterNotNull().first { it.type == Material.BOOK }
//        val enchantedBook: ItemStack = originMatrix.filterNotNull().first { it.type == Material.ENCHANTED_BOOK }
//
//        Bukkit.getRegionScheduler()
//            .runDelayed(Main.configManager.javaPlugin, event.view.player.location, Consumer<ScheduledTask>() {
//                if (normalBook.amount > 0) {
//                    val putBook = normalBook.clone()
//                    putBook.amount = normalBook.amount - 1
//                    event.inventory.setItem(originMatrix.indexOf(normalBook) + 1, putBook)
//                }
//                event.inventory.setItem(originMatrix.indexOf(enchantedBook) + 1, enchantedBook.clone())
//            }, 1)
//    }
}