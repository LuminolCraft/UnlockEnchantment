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

/**
 * 附魔简化合成监听器
 *
 * ## 业务目标
 * 玩家在工作台放入 1 本附魔书（ENCHANTED_BOOK）+ 1 本普通书（BOOK），
 * 合成结果 = 附魔书的副本，但**只消耗普通书**，附魔书会原样保留在合成格子中。
 *
 * 这样玩家可以低成本地把附魔书上的附魔"复制"到普通书上进行后续合并，
 * 避免反复消耗高级附魔书（如经验修补、抢夺 III、耐久 III 等）。
 *
 * ## 实现拆分
 * 由于原版合成会同时消耗两本书，本插件用两个事件接管整个流程：
 * - [onPrepareCraft]：在"准备结果"阶段，把结果槽设为附魔书副本（仅用于 UI 展示，让玩家看到会得到什么）
 * - [onCraft]：在玩家点击取结果时，取消原版合成，自定义扣减普通书、保留附魔书、把副本放到光标
 */
class CraftListener : Listener {
    /**
     * 监听合成台"准备结果"事件。
     *
     * 触发时机：玩家摆好材料、结果槽尚未最终确定时（每次材料变化都会触发一次）。
     * 作用：让合成台 UI 提前显示结果 = 附魔书的副本，提示玩家这次合成会得到什么。
     *
     * priority = [EventPriority.MONITOR]：本插件只在最后阶段补设 result 用于展示。
     *
     * @param event 由 Bukkit 触发的合成准备事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPrepareCraft(event: PrepareItemCraftEvent) {
        // 校验一：合成矩阵中恰好 2 个非空物品 + 配置开启了附魔简化合成。
        // matrix 是合成台 9 格材料数组，可能含 null 和空气物品，需要先 filterNotNull 再过滤 isEmpty。
        // 任一条件不满足即直接返回（不归本插件管，交给原版合成处理）。
        if (event.inventory.matrix.filterNotNull()
                .filter { !it.isEmpty }.size != 2 || !Main.configManager.isEnchantmentSimplify
        ) return
        // 校验二：两件物品必须分别是 1 本 ENCHANTED_BOOK（附魔书）和 1 本 BOOK（普通书）。
        // 任意一个缺失就说明不是本插件处理的配方，直接返回。
        if (event.inventory.matrix.firstOrNull { it?.type == Material.ENCHANTED_BOOK } == null ||
            event.inventory.matrix.firstOrNull { it?.type == Material.BOOK } == null) return
        // 重新过滤出干净的 2 个非空物品列表，便于下面按类型取出
        val matrix: List<ItemStack> = event.inventory.matrix.filterNotNull().filter { !it.isEmpty }
        // 取出附魔书，并克隆一份作为结果展示（克隆是为了不修改玩家放在格子里的原物品）
        val craftEnchantedBook: ItemStack = matrix.first { it.type == Material.ENCHANTED_BOOK }
        val resultEnchantedBook: ItemStack = craftEnchantedBook.clone()
        // 把结果写入合成台的 result 槽，UI 上就会显示这本附魔书
        event.inventory.result = resultEnchantedBook
    }

    /**
     * 监听玩家点击合成台 UI 的操作事件。
     *
     * 触发时机：玩家在合成台 UI 中点击任意槽位（含结果槽）时。
     * 作用：当玩家点击结果槽(slot=0)取走附魔书时，接管合成流程：
     *   1. 取消原版合成（原版会按默认配方同时消耗附魔书和普通书）
     *   2. 延迟 1 tick 后：扣减普通书 1 本，把附魔书原样放回原格
     *   3. 立即把附魔书副本放到玩家光标上（让玩家"拿起了"结果物品）
     *
     * priority = [EventPriority.MONITOR]：在最后阶段拦截，确保在其他插件处理完后做最终取消。
     *
     * @param event 由 Bukkit 触发的库存点击事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onCraft(event: InventoryClickEvent) {
        // 校验一：配置开启 simplify + 当前打开的是合成台 + 点击者必须是玩家。
        // 三个条件用 || 短路，任一不满足即直接放行（不归本插件管）。
        if (!Main.configManager.isEnchantmentSimplify || event.inventory !is CraftingInventory || event.whoClicked !is Player) return
        // 校验二：只处理点击结果槽(slot == 0)的情况，其他槽位点击交给原版处理。
        if (event.slot != 0) return
        // 关键：取消原版合成逻辑。否则原版会按默认配方消耗两本书，
        // 附魔书会被一起消耗掉，违背"附魔书保留"的业务规则。
        event.isCancelled = true
        // 校验三：玩家光标必须为空，否则结果物品放不下，本次合成无效。
        if(!event.view.cursor.isEmpty) return
        // 取出合成矩阵的原始引用（数组下标与合成台格子一一对应，后面放回物品要用 index）
        val originMatrix: Array<out ItemStack?> = (event.inventory as CraftingInventory).matrix
        // 校验四：再次确认材料正确（防止与 onPrepareCraft 之间的竞争条件或外部修改）。
        if (originMatrix.firstOrNull { it?.type == Material.ENCHANTED_BOOK } == null ||
            originMatrix.firstOrNull { it?.type == Material.BOOK } == null) return
        // 分别取出普通书和附魔书
        val normalBook: ItemStack = originMatrix.filterNotNull().first { it.type == Material.BOOK }
        val enchantedBook: ItemStack = originMatrix.filterNotNull().first { it.type == Material.ENCHANTED_BOOK }
        // 延迟 1 tick 后修改合成格子内容。
        // 为什么用 Player.scheduler.runDelayed：
        //   - 这是 Paper/Folia 提供的"区域调度器"，按玩家所在区域调度任务，
        //     Folia 没有 Bukkit 的全局调度器（BukkitScheduler），必须用这种基于实体/区域的调度 API。
        //   - 为什么延迟 1 tick：在事件处理中直接修改 inventory 会被原版合成逻辑覆盖，
        //     必须等当前 tick 事件处理结束、原版合成流程跑完后，再修改格子内容才能生效。
        (event.whoClicked as Player).scheduler.runDelayed(Main.configManager.javaPlugin, {
            // 普通书数量 > 0 时，扣减 1 本后放回原格。
            // originMatrix.indexOf(normalBook) + 1 是因为：
            //   matrix 数组下标从 0 开始（0..8），而合成台 setItem 的槽位号从 1 开始（1..9）。
            if (normalBook.amount > 0) {
                val putBook: ItemStack = normalBook.clone()
                putBook.amount -= 1
                event.inventory.setItem(originMatrix.indexOf(normalBook) + 1, putBook)
            }
            // 附魔书原样克隆一份放回原格，保证数量、附魔、NBT 都不变（即"不消耗附魔书"）
            val putEnchantedBook: ItemStack = enchantedBook.clone()
            event.inventory.setItem(originMatrix.indexOf(enchantedBook) + 1, putEnchantedBook)
        }, null,1L)
        // 立即把附魔书的副本放到玩家光标上（玩家会看到"拿起了"结果物品，无需等待延迟回调）
        event.view.setCursor(enchantedBook.clone())
    }
}
