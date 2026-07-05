package org.luminolcraft.unlockEnchantment

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta

class AnvilListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        val result: ItemStack = event.result ?: return
        if (event.inventory.firstItem == null || event.inventory.secondItem == null) return
        val firstItemEnchants: Map<Enchantment?, Int?>? =
            if (event.inventory.firstItem?.getType() == Material.ENCHANTED_BOOK
                && event.inventory.firstItem?.itemMeta is EnchantmentStorageMeta
            )
                event.inventory.firstItem?.itemMeta?.enchants
            else
                event.inventory.firstItem?.getEnchantments()
        val secondItemEnchants: Map<Enchantment?, Int?>? =
            if (event.inventory.secondItem?.getType() == Material.ENCHANTED_BOOK
                && event.inventory.secondItem?.itemMeta is EnchantmentStorageMeta
            )
                event.inventory.secondItem?.itemMeta?.enchants
            else
                event.inventory.secondItem?.getEnchantments()

        if (firstItemEnchants == null || secondItemEnchants == null) return


        val ItemEnchants: MutableMap<Enchantment?, Int?> = mutableMapOf<Enchantment?, Int?>()
        for (s in secondItemEnchants) {
            if (s.key?.maxLevel == 1) {
                ItemEnchants.put(s.key, s.key?.startLevel)
                continue
            }

            if (firstItemEnchants.containsKey(s.key)) {
                if (firstItemEnchants[s.key]!! <= 10) {
                    if (firstItemEnchants[s.key]!! <= s.value!!) {
                        ItemEnchants.put(s.key, s.value)
                        continue
                    }
                    ItemEnchants.put(s.key, firstItemEnchants[s.key]!!)
                    continue
                }

            }
        }
    }

}