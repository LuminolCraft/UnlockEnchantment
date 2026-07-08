/**
 * UnlockEnchantment 插件入口文件
 *
 * 所在包：org.luminolcraft.unlockEnchantment
 *
 * 整体职责：
 * 本文件是 Minecraft Paper/Folia 服务端插件 UnlockEnchantment 的入口点（Entry Point）。
 * 所谓"入口点"，就是服务端在加载插件时最先识别、最先实例化、最先调用的类。
 *
 * 插件核心功能：
 * 1. 解除原版铁砧附魔等级上限（让玩家能合成超过 40 级的附魔）
 * 2. 提供附魔简化合成（附魔书 + 普通书 = 附魔书，仅消耗普通书）
 *
 * 技术栈：Kotlin + Folia API 26.1.2 + Gradle (Shadow)
 *
 * 注意：本文件只负责插件的生命周期管理与组件注册，具体业务逻辑分散在
 * AnvilListener（铁砧事件）、CraftListener（合成事件）、ConfigManager（配置管理）等类中。
 */
package org.luminolcraft.unlockEnchantment

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.luminolcraft.unlockEnchantment.config.ConfigManager


/**
 * 插件主类 Main
 *
 * 继承自 [JavaPlugin]：这是 Bukkit/Paper/Folia 插件的标准基类。
 * 服务端加载插件时，会读取 plugin.yml 中指定的 main 类路径，然后通过反射
 * 实例化这个类（要求有一个无参构造函数，这里 Kotlin 默认提供）。
 *
 * 作为插件主类的角色：
 * - 持有插件的生命周期方法（[onEnable]、[onDisable]），由服务端在合适时机调用
 * - 负责初始化全局组件（如 ConfigManager）
 * - 负责注册事件监听器（如 AnvilListener、CraftListener）
 *
 * 生命周期方法调用时机：
 * - [onEnable]：插件被启用时调用（服务器启动时，或管理员用 /reload 命令重载时）
 * - [onDisable]：插件被禁用时调用（服务器关闭时，或管理员卸载插件时）
 *
 * 注意：Main 类本身不实现具体的附魔解锁逻辑，它只是一个"启动器"和"装配器"。
 */
class Main : JavaPlugin() {

    /**
     * 插件启用时由服务端调用的生命周期方法
     *
     * 调用时机：服务器启动过程中（或插件被 reload 时），服务端会在实例化 Main 类之后
     * 立即调用此方法。此时插件已经"准备好"可以被使用，因此所有初始化工作都应放在这里。
     *
     * 本方法完成三件事：
     * 1. 加载配置文件（ConfigManager）
     * 2. 注册事件监听器（AnvilListener、CraftListener）
     * 3. 输出加载成功日志
     */
    override fun onEnable() {
        // 实例化 ConfigManager 并传入 config 与 this：
        // - config 是 JavaPlugin 提供的 FileConfiguration 对象，对应插件目录下的 config.yml
        // - this 指向当前 Main 实例（即插件实例），ConfigManager 可能需要用它获取 logger、dataFolder 等
        // 此处将构造好的 ConfigManager 赋值给伴生对象中的 configManager 全局变量，供其他类访问
        configManager = ConfigManager(config, this)

        // 调用 loadConfig()：实际读取 config.yml 的内容到内存，
        // 通常会解析各个配置项（如附魔等级上限、合成规则等）供运行时查询
        configManager.loadConfig()

        // 向服务端的 PluginManager 注册 AnvilListener（铁砧事件监听器）：
        // - AnvilListener() 是无参构造，监听玩家使用铁砧时的事件（如 PrepareAnvilEvent、AnvilDamageEvent 等）
        // - this 作为第二个参数表示该监听器归属于本插件，插件卸载时会自动取消注册
        Bukkit.getPluginManager().registerEvents(AnvilListener(), this)

        // 注册 CraftListener（合成事件监听器）：
        // - 监听玩家在工作台/合成格中的合成事件（如 PrepareItemCraftEvent）
        // - 用于实现"附魔书 + 普通书 = 附魔书"的简化合成功能
        Bukkit.getPluginManager().registerEvents(CraftListener(), this)

        // 输出 "Loaded" 日志到服务端控制台：
        // - logger 继承自 JavaPlugin，会自动带上插件名前缀（如 [UnlockEnchantment] Loaded）
        // - 用于确认插件已成功加载，方便管理员排查问题
        logger.info("Loaded")
    }

    /**
     * 插件禁用时由服务端调用的生命周期方法
     *
     * 调用时机：服务器关闭过程中，或管理员卸载/重载插件时。
     * 通常在此方法中执行资源清理工作，例如：
     * - 关闭数据库连接
     * - 保存未持久化的数据
     * - 取消定时任务
     *
     * 当前实现：无任何清理逻辑（业务上不需要），保留空方法体。
     * 注意：Bukkit 框架会自动取消本插件注册的监听器和任务，无需手动处理。
     */
    override fun onDisable() {
        // Plugin shutdown logic
        // 当前没有需要清理的资源，故保留空实现
    }

    /**
     * 伴生对象（companion object）
     *
     * Kotlin 语法说明：
     * - companion object 类似于 Java 的 static，但更强大（它本身是一个单例对象）
     * - 定义在类内部的 companion object，其成员可以通过类名直接访问，例如 `Main.configManager`
     * - 一个类只能有一个 companion object
     *
     * 为什么把 configManager 放在这里？
     * - 因为 AnvilListener、CraftListener 等其他类需要在运行时读取配置，
     *   但它们无法直接拿到 Main 的实例引用
     * - 通过伴生对象提供一个"全局访问点"，其他类只需 `Main.configManager` 即可获取配置管理器
     *
     * 关于 lateinit：
     * - lateinit 表示"延迟初始化"，告诉编译器：这个变量现在不赋值，稍后一定会在使用前赋值
     * - 为什么需要？因为 configManager 在 onEnable() 中才被赋值（服务端先实例化 Main，
     *   再调用 onEnable），无法在声明时直接初始化
     * - 如果不加 lateinit，Kotlin 会要求变量必须可空（ConfigManager?）或在声明时初始化，
     *   这两种方式都不符合需求
     * - lateinit 的代价：如果在赋值前访问该变量，会抛出 UninitializedPropertyAccessException
     */
    companion object {
        lateinit var configManager: ConfigManager
    }
}
