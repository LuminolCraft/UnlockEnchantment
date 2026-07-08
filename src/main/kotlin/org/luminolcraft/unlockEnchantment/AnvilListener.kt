package org.luminolcraft.unlockEnchantment

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta

/**
 * 铁砧附魔合并监听器：解除原版铁砧"过于昂贵"限制的核心实现。
 *
 * ## 业务目标
 * 原版铁砧在合并花费 ≥ 40 级时会显示"过于昂贵"并阻止玩家取出结果。
 * 本监听器在玩家打开铁砧界面、放入物品时，重新计算两个物品合并后的附魔，
 * 绕过原版上限，让高等级附魔能够继续合并升级。
 *
 * ## 工作流程概述
 * 1. 将铁砧 `maximumRepairCost` 设为 `Int.MAX_VALUE`，解除"过于昂贵"限制
 * 2. 分别读取左侧（firstItem）和右侧（secondItem）物品的附魔
 * 3. 遍历第二物品附魔，按规则合并到结果中
 * 4. 限制最大花费、发送昂贵提示消息
 * 5. 应用特殊附魔上限覆盖
 * 6. 用 `addUnsafeEnchantment` 写入 result，绕过原版兼容性检查
 */
class AnvilListener : Listener {

    /**
     * 监听铁砧准备事件。
     *
     * `PrepareAnvilEvent` 在玩家放入/移除铁砧内物品时触发，
     * 允许插件修改最终输出（result）和花费（repairCost）。
     *
     * `priority = EventPriority.MONITOR` 表示本监听器在其他插件（默认/低优先级）之后执行，
     * 用于在他人修改的基础上做最终调整。MONITOR 通常只读，但本插件需要重写 result，
     * 因此这里显式覆盖结果。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        // 1. 解除"过于昂贵"限制：把铁砧的最大允许花费抬到 Int 最大值，
        //    这样无论 repairCost 多高都不会被原版逻辑拦截。
        event.view.maximumRepairCost = Int.MAX_VALUE

        // 取出当前铁砧的预览结果；若为 null（如未放入任何物品）直接返回，无需处理。
        val result: ItemStack = event.result ?: return

        // 左侧物品（被附魔/被修复的目标）。
        val firstItem: ItemStack? = event.inventory.firstItem
        // 右侧物品（被牺牲物品，通常是附魔书或同款装备）。
        val secondItem: ItemStack? = event.inventory.secondItem

        // 第二物品为 null 时无法进行合并，提前返回。
        if (event.inventory.secondItem == null) return

        // 任一物品为空、或插件被配置关闭时，都不处理，交给原版逻辑。
        if (firstItem!!.isEmpty || event.inventory.secondItem!!.isEmpty || !Main.configManager.isPluginEnabled) return

        // 2. 读取两个物品的附魔。
        //    附魔书（ENCHANTED_BOOK）的附魔存储在 NBT 的 StoredEnchantments 字段里，
        //    不能用 ItemStack.enchantments 直接获取（那只能拿到空 Map），
        //    必须通过 EnchantmentStorageMeta.storedEnchants 才能读到。
        //    普通装备/工具的附魔则直接挂在 ItemStack 上，用 enchantments 即可。
        val firstItemEnchants: Map<Enchantment?, Int?>? =
            if (firstItem.type == Material.ENCHANTED_BOOK
                && firstItem.itemMeta is EnchantmentStorageMeta
            ) {
                // 附魔书：从 EnchantmentStorageMeta 取出存储的附魔。
                (firstItem.itemMeta as? EnchantmentStorageMeta)?.storedEnchants
            } else {
                // 普通物品：直接取 ItemStack 上的附魔。
                firstItem.enchantments
            }

        val secondItemEnchants: Map<Enchantment?, Int?>? =
            if (secondItem?.type == Material.ENCHANTED_BOOK
                && secondItem.itemMeta is EnchantmentStorageMeta
            ) {
                // 附魔书：同上，从 EnchantmentStorageMeta 取出存储的附魔。
                (secondItem.itemMeta as? EnchantmentStorageMeta)?.storedEnchants
            } else {
                // 普通物品：直接取 ItemStack 上的附魔。
                secondItem?.enchantments
            }

        // 两个物品都没有附魔时无需合并，交给原版处理。
        if (firstItemEnchants == null && secondItemEnchants == null) return

        // 用于保存合并后的附魔结果（最终会写入 result）。
        val itemEnchants: MutableMap<Enchantment?, Int?> = mutableMapOf<Enchantment?, Int?>()

        // 3. 合并循环：遍历第二物品的每一条附魔，按规则决定结果等级。
        if (secondItemEnchants != null) {
            for (s in secondItemEnchants) {
                // 分支 A：黑名单附魔——直接保留第二物品的原值，不做任何特殊处理。
                if (Main.configManager.blackListEnchantments.contains(s.key)) {
                    itemEnchants[s.key] = s.value
                    continue
                }

                // 分支 B：单级附魔（maxLevel == 1，如经验修补、无限、绑定诅咒）。
                //         这类附魔没有等级递增概念，结果固定为 startLevel（通常为 1）。
                if (s.key?.maxLevel == 1) {
                    itemEnchants[s.key] = s.key?.startLevel
                    continue
                }

                // 分支 C：第一物品也拥有该附魔，需要做等级合并。
                if (firstItemEnchants!!.containsKey(s.key)) {
                    // C-1：第一物品该附魔等级 ≤ 10，或两物品类型相同（书+书 / 装备+同款装备）。
                    //      这是"常规合并"路径：
                    //        - 第二物品等级更高 → 取第二物品的等级；
                    //        - 两者同级 → 升一级（例如两个锋利 V → 锋利 VI）；
                    //        - 第一物品等级更高 → 保持第一物品的等级不变。
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

                    // C-2：装备 + 附魔书，且配置启用了"简化模式"（isEnchantmentSimplify）。
                    //      这是"跨等级合并"路径：当装备上的附魔等级较高（>10）时，
                    //      用 truncateOnes 截断个位后比较：
                    //        - 第二物品等级更高 → 取第二物品的等级；
                    //        - 两者截断后相同（如 13 与 12 都截断为 10）→ 在第一物品等级上 +1；
                    //        - 否则保持第一物品的等级不变。
                    //      这样允许等级接近的附魔跨级合并，避免"必须完全同级才能升级"的硬性限制。
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
                        // 第二物品等级更高时，直接取第二物品的等级。
                        if (firstItemEnchants[s.key]!! < s.value!!) {
                            itemEnchants[s.key] = s.value
                            continue
                        }
                        // 截断个位后相同 → 在第一物品等级上 +1（允许跨等级合并升级）。
                        if (truncateOnes(firstItemEnchants[s.key]!!) == truncateOnes(secondItemEnchants[s.key]!!)) {
                            itemEnchants[s.key] = firstItemEnchants[s.key]!! + 1
                            continue
                        }
                        // 其他情况保持第一物品的等级不变。
                        itemEnchants[s.key] = firstItemEnchants[s.key]!!
                        continue

                    }
                }
            }

            // 补齐：第二物品中存在但第一物品没有的附魔，未在上面的合并循环中处理，
            //       这里直接加入结果，保证第二物品的所有附魔都不丢失。
            for (s in secondItemEnchants) {
                if (itemEnchants[s.key] != null) {
                    continue
                }
                itemEnchants[s.key] = s.value
            }
        }

        // 4. 最大花费限制：若配置了 maximumLevelCost（!= -1）且当前 repairCost 已达到或超过它，
        //    则把花费限制为该上限，避免极高等级合并导致经验花费失控。
        if (event.view.repairCost >= Main.configManager.maximumLevelCost && Main.configManager.maximumLevelCost != -1) {
            event.view.repairCost = Main.configManager.maximumLevelCost
        }

        // 5. 昂贵提示：花费 > 39 时（原版"过于昂贵"阈值附近），向玩家发送提示消息。
        //    消息使用 MiniMessage 格式，其中 {level} 占位符会被替换为实际花费等级。
        if (event.view.repairCost > 39) {
            event.view.player.sendMessage(
                MiniMessage.miniMessage().deserialize(
                    Main.configManager.expensiveEnchantMessage.replace(
                        "{level}",
                        event.view.repairCost.toString(), false
                    )
                )
            )
        }

        // 6. 写入结果并应用特殊附魔上限覆盖：
        //    若该附魔在 specialEnchantments 中，或结果等级超过其自定义上限 maximumLevels，
        //    则将结果等级设为 specialEnchantments[key].maximumLevels。
        //    最后用 addUnsafeEnchantment 将结果写入 result，绕过原版兼容性检查
        //    （原版 addEnchantment 会因等级超过 maxLevel 而抛异常）。
        for (e in itemEnchants) {
            e.let {
                if (Main.configManager.specialEnchantments.containsKey(
                        it.key
                    ) ||
                    Main.configManager.specialEnchantments[it.key]?.maximumLevels?.let { it1 -> e.value!! > it1 } == true
                ) {
                    itemEnchants[it.key] = Main.configManager.specialEnchantments[it.key]!!.maximumLevels
                }
                result.addUnsafeEnchantment(it.key!!, itemEnchants[it.key]!!)
            }
        }


    }

    companion object {
        /**
         * 截断一个整数的个位数（向下取整到最近的 10 的倍数）。
         *
         * 数学含义：`number / 10 * 10`，先整除去掉个位、再乘 10 补回低位零。
         * 例如：
         *   - 13 → 10
         *   - 25 → 20
         *   - 9  → 0
         *   - 100 → 100
         *
         * 业务用途：在"简化模式"（装备 + 附魔书合并）下，让等级接近的附魔被视为"同级"，
         * 从而允许跨等级合并升级（例如装备上 13 级锋利 + 书上 12 级锋利 → 截断后都是 10 → 合并升到 14）。
         */
        fun truncateOnes(number: Int): Int {
            return number / 10 * 10
        }
    }

}
