package org.luminolcraft.unlockEnchantment

import org.bukkit.Material
import org.bukkit.Tag
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
//        println("Event Handled")
        val result: ItemStack = event.result ?: return
        val firstItem: ItemStack? = event.inventory.firstItem
        val secondItem: ItemStack? = event.inventory.secondItem
//        println("If no more log,bugs.Stage 1")
        if (firstItem!!.isEmpty || event.inventory.secondItem!!.isEmpty || !Main.configManager.isPluginEnabled) return
//        println("If no more log,bugs.Stage 2")
//        val firstItemEnchants: Map<Enchantment?, Int?>?
//        firstItemEnchants = if (event.inventory.firstItem?.type == Material.ENCHANTED_BOOK
//            && event.inventory.firstItem!!.itemMeta is EnchantmentStorageMeta
//        )
//            (event.inventory.firstItem?.itemMeta as EnchantmentStorageMeta).enchants
//        else
//            event.inventory.firstItem?.enchantments
//        val secondItemEnchants: Map<Enchantment?, Int?>?
//        secondItemEnchants = if (event.inventory.secondItem?.type == Material.ENCHANTED_BOOK
//            && event.inventory.secondItem!!.itemMeta is EnchantmentStorageMeta
//        )
//            (event.inventory.secondItem?.itemMeta as EnchantmentStorageMeta).enchants
//        else
//            event.inventory.secondItem?.enchantments

        //DeepSeek solution
        val firstItemEnchants: Map<Enchantment?, Int?>? =
            if (firstItem.type == Material.ENCHANTED_BOOK
                && firstItem.itemMeta is EnchantmentStorageMeta
            ) {
                (firstItem.itemMeta as? EnchantmentStorageMeta)?.storedEnchants
            } else {
                firstItem.enchantments
            }

        val secondItemEnchants: Map<Enchantment?, Int?>? =
            if (secondItem?.type == Material.ENCHANTED_BOOK
                && secondItem.itemMeta is EnchantmentStorageMeta
            ) {
                (secondItem.itemMeta as? EnchantmentStorageMeta)?.storedEnchants
            } else {
                secondItem?.enchantments
            }
//        println("If no more log,bugs.Stage 3")
        if (firstItemEnchants == null && secondItemEnchants == null) return


        val itemEnchants: MutableMap<Enchantment?, Int?> = mutableMapOf<Enchantment?, Int?>()
//        println("If no more log,bugs.Stage 4")
        if (secondItemEnchants != null) {
            for (s in secondItemEnchants) {
                if (Main.configManager.blackListEnchantments.contains(s.key)) {
                    itemEnchants[s.key] = s.value
                    continue
                }
                //            println("Run into FOR1.")
                if (s.key?.maxLevel == 1) {
                    //                println("Run into IF1.")
                    itemEnchants[s.key] = s.key?.startLevel
                    continue
                }

                if (firstItemEnchants!!.containsKey(s.key)) {
                    //                println("Run into IF2.")
                    if (firstItemEnchants[s.key]!! <= 10 || firstItem!!.type == secondItem!!.type) {
                        if (firstItemEnchants[s.key]!! < s.value!!) {
                            itemEnchants[s.key] = s.value
                            continue
                        }
                        if (firstItemEnchants[s.key]!! == s.value!!) {
                            itemEnchants[s.key] = s.value!! + 1
                            continue
                        }
                        itemEnchants[s.key] = firstItemEnchants[s.key]!!
                        continue
                    }
                    //装备+书
                    if (
                        (Tag.ITEMS_PICKAXES.isTagged(firstItem.type) ||
                                Tag.ITEMS_AXES.isTagged(firstItem.type) ||
                                Tag.ITEMS_SHOVELS.isTagged(firstItem.type) ||
                                Tag.ITEMS_SWORDS.isTagged(firstItem.type) ||
                                Tag.ITEMS_HOES.isTagged(firstItem.type) ||
                                Tag.ITEMS_CHEST_ARMOR.isTagged(firstItem.type) ||
                                Tag.ITEMS_FOOT_ARMOR.isTagged(firstItem.type) ||
                                Tag.ITEMS_LEG_ARMOR.isTagged(firstItem.type) ||
                                Tag.ITEMS_HEAD_ARMOR.isTagged(firstItem.type)) && secondItem.type == Material.ENCHANTED_BOOK &&
                        Main.configManager.isEnchantmentSimplify
                    ) {
                        if (firstItemEnchants[s.key]!! < s.value!!) {
                            itemEnchants[s.key] = s.value
                            continue
                        }
                        if (truncateOnes(firstItemEnchants[s.key]!!) == truncateOnes(secondItemEnchants[s.key]!!)) {
//                            println("firstItemEnchants[${s.key}] is truncated,value is ${truncateOnes(firstItemEnchants[s.key]!!)},secondItemEnchants[${s.key}] is truncated,value is ${truncateOnes(secondItemEnchants[s.key]!!)}")
                            itemEnchants[s.key] = firstItemEnchants[s.key]!! + 1
                            continue
                        }
                        itemEnchants[s.key] = firstItemEnchants[s.key]!!
                        continue

                    }
                }
            }
            //补齐第二个物品内存在但第一个物品不存在的附魔
            for (s in secondItemEnchants) {
                if (itemEnchants[s.key] != null) {
                    continue
                }
                itemEnchants[s.key] = s.value
            }
        }

        //最大花费 总体
        if (event.view.repairCost >= Main.configManager.maximumLevelCost && Main.configManager.maximumLevelCost != -1) {
            event.view.repairCost = Main.configManager.maximumLevelCost
        }

        for (e in itemEnchants) {
            e.let {
                if (Main.configManager.specialEnchantments.containsKey(it.key) ||
                    e.value!! > Main.configManager.specialEnchantments[it.key]!!.maximumLevels
                ) {
                    itemEnchants[it.key] = Main.configManager.specialEnchantments[it.key]!!.maximumLevels
                }
                result.addUnsafeEnchantment(it.key!!, itemEnchants[it.key]!!)
            }
        }
//        println("All functions passed.")

    }

    companion object {
        fun truncateOnes(number: Int): Int {
            return number / 10 * 10
        }
    }

}