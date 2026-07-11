/**
 * 配置管理相关类的包。
 *
 * 该包下的 [ConfigManager] 是 UnlockEnchantment 插件的配置管理中枢，
 * 负责 `plugins/UnlockEnchantment/config.yml` 文件的创建、默认值写入、
 * 读取以及向内存字段映射，并为事件监听器提供统一的配置访问入口。
 */
package org.luminolcraft.unlockEnchantment.config

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * 插件配置管理器。
 *
 * 该类是 UnlockEnchantment 插件与 `config.yml` 交互的唯一入口，承担以下职责：
 * 1. 在首次启动时创建 `plugins/UnlockEnchantment/config.yml` 并写入默认配置项；
 * 2. 将配置文件中的键值映射为内存中可直接使用的字段（如布尔开关、整数上限、附魔黑名单等）；
 * 3. 提供重载方法，支持在不重启服务器的情况下刷新配置；
 * 4. 暴露内存字段供事件监听器（如铁砧事件监听器）读取使用。
 *
 * @param config 由 [JavaPlugin] 提供的 [FileConfiguration] 实例，
 *               通常通过 `javaPlugin.getConfig()` 获取，用于读写 YAML 配置文件。
 * @param javaPlugin 插件主类的实例，主要用于定位插件的数据文件夹
 *                   （`plugins/UnlockEnchantment`），以便创建或读取 `config.yml`。
 */
class ConfigManager(val config: FileConfiguration, val javaPlugin: JavaPlugin) {
    /**
     * 配置文件在本地的物理路径，固定指向 `plugins/UnlockEnchantment/config.yml`。
     * 当插件首次启动且该文件不存在时，会在 [initConfig] 中被自动创建。
     */
    val configFile: File = File(javaPlugin.dataFolder, "config.yml")

    /**
     * 插件功能总开关。
     * 对应配置项 `enabled`，为 `false` 时插件的所有附魔等级上限解除功能都不会生效。
     */
    var isPluginEnabled: Boolean = false

    /**
     * 是否启用附魔简化合成。
     * 对应配置项 `simplify-enchantment`，开启后可简化铁砧中附魔的合并规则。
     */
    var isEnchantmentSimplify: Boolean = false

    /**
     * 铁砧合成时允许的最大等级花费。
     * 对应配置项 `maximum-level-cost`，设置为 `-1` 表示不限制花费（解除原版 40 级上限）。
     */
    var maximumLevelCost: Int = -1

    /**
     * 从配置项 `blacklist` 中读取的原始字符串列表（如 `"SHARPNESS"`）。
     * 该字段仅保存原始字符串，尚未转换为 [Enchantment] 对象，
     * 转换结果存放在 [blackListEnchantments] 中。
     */
    private var blacklistStr: List<String> = Lists.newArrayList()

    /**
     * 转换后的黑名单附魔对象列表。
     * 列表中的每个附魔在插件处理铁砧合成时都会被忽略（即不被插件干预其上限）。
     * 元素可能为 `null`（当配置中填写的字符串无法匹配到任何附魔时）。
     */
    var blackListEnchantments: MutableList<Enchantment?> = Lists.newArrayList()

    /**
     * 特殊附魔的自定义上限映射表：[Enchantment] → [SpecialEnchantments]。
     * 对应配置项 `special-enchantment-setting`，用于为指定附魔单独设置一个
     * 与原版不同的最大等级（例如把锋利上限设为 5）。
     */
    var specialEnchantments: MutableMap<Enchantment, SpecialEnchantments> = Maps.newHashMap()

    /**
     * 当铁砧合成花费等级达到 40 及以上时发送给玩家的提示消息。
     * 对应配置项 `expensive-enchant-message`，支持 MiniMessage 格式
     * （例如 `<RED>`、`<green>`、`<hover:show_item:...>` 等标签）。
     */
    var expensiveEnchantMessage: String = String()

    var reloadMessage: String = String()

    /**
     * 初始化配置文件。
     *
     * 该方法的核心职责是确保 `plugins/UnlockEnchantment/config.yml` 存在，
     * 并且其中包含所有必要的默认配置项及对应注释。其执行流程为：
     * 1. 若配置文件不存在，先创建其父目录再创建空文件；
     * 2. 将文件内容加载进内存中的 [config] 对象；
     * 3. 逐项检查每个配置键是否存在，若缺失则写入默认值并附上说明注释；
     * 4. 调用 [config.save] 将（可能被新增默认值的）配置持久化回磁盘。
     *
     * 该方法不会读取配置值到字段，字段映射由 [loadConfig] 负责。
     */
    private fun initConfig() {
        var shouldSave = false
        // 首次启动时配置文件可能尚未创建：先确保父目录存在，再创建空文件
        if (!configFile.exists()) {
            configFile.parentFile.mkdirs()
            configFile.createNewFile()
            shouldSave = true
        }
        // 将磁盘上的 YAML 内容加载到内存的 FileConfiguration 对象中
        config.load(configFile)
        // 以下逐项检查配置键是否存在，缺失时写入默认值和说明注释，
        // 这样既兼容老版本配置文件，也能让新用户直接看到完整可读的 config.yml。
        if (config.get("enabled") == null) {
            // 插件功能总开关，默认开启（true）
            config.set("enabled", true)
            config.setComments("enabled", listOf("Set whether to enable the plugin's function"))
        }
        if (config.get("simplify-enchantment") == null) {
            // 附魔简化合成开关，默认开启（true）
            config.set("simplify-enchantment", true)
            config.setComments("simplify-enchantment", listOf("Set whether to simplify the enchantment"))
        }
        if (config.get("maximum-level-cost") == null || config.getInt("maximum-level-cost") < -1) {
            // 铁砧最大等级花费，默认 -1 表示不限制（解除原版 40 级上限）；
            // 若用户填入了小于 -1 的非法值，也回退为 -1
            config.set("maximum-level-cost", -1)
            config.setComments(
                "maximum-level-cost",
                listOf("Set the value of maximum level cost", "Set -1 to make the cost unlimited")
            )
        }
        if (config.get("blacklist") == null) {
            // 附魔黑名单：列表中的附魔将不被本插件处理，默认为空列表
            config.set("blacklist", listOf(null))
            config.setComments(
                "blacklist", listOf(
                    "Set which enchantment should be ignored",
                    "making it didn't be applied in this plugin",
                    "To check the name of each enchantment,",
                    "See: https://jd.papermc.io/folia/26.1.2/org/bukkit/enchantments/Enchantment.html",
                    "Example:",
                    "blacklist:",
                    "  - SHARPNESS"
                )
            )

        }
        if (config.get("special-enchantment-setting") == null) {
            // 特殊附魔上限设置：可为指定附魔单独定义一个最大等级，默认为空列表
            config.set("special-enchantment-setting", listOf(null))
            config.setComments(
                "special-enchantment-setting", listOf(
                    "Set which enchantment should be specially adjusted",
                    "Example:",
                    "special-enchantment-setting:",
                    "  SHARPNESS:",
                    "    maximum-level: 5"
                )
            )
        }
        if (config.get("expensive-enchant-message") == null) {
            // 当铁砧花费等级达到 40 及以上时发送给玩家的提示消息，支持 MiniMessage 格式
            config.set(
                "expensive-enchant-message",
                "<hover:show_item:enchanted_book></hover><RED>超出原版附魔显示，附魔所需等级为 <green>{level}</green>"
            )
            config.setComments(
                "expensive-enchant-message",
                listOf("Set the message displayed when the cost level reaches to 40 and above.")
            )
        }
        if (config.get("reload-message") == null) {
            config.set("reload-message", "<green>重载配置文件完成.")
            config.setComments("reload-message", listOf("Set the message displayed when reload"))
        }
        // 将可能新增的默认配置项写回磁盘，保证 config.yml 与插件期望一致

        if (shouldSave) config.save(configFile)

    }

    /**
     * 将配置文件中的键值加载到内存字段。
     *
     * 该方法先调用 [initConfig] 确保文件与默认值就绪，然后把各项配置读取到
     * [ConfigManager] 的公开属性中，供监听器在运行时直接访问。具体映射如下：
     * - `enabled` → [isPluginEnabled]
     * - `simplify-enchantment` → [isEnchantmentSimplify]
     * - `maximum-level-cost` → [maximumLevelCost]
     * - `blacklist` → [blacklistStr]，再逐项转换为 [blackListEnchantments]
     * - `special-enchantment-setting` → [specialEnchantments]
     * - `expensive-enchant-message` → [expensiveEnchantMessage]
     */
    fun loadConfig() {
        // 先保证配置文件与默认值存在，再进行读取
        initConfig()
        // 读取三个标量配置项，第二个参数为读取失败时的兜底默认值
        isPluginEnabled = config.getBoolean("enabled", true)
        isEnchantmentSimplify = isPluginEnabled && config.getBoolean("simplify-enchantment", true)
        maximumLevelCost = config.getInt("maximum-level-cost", -1)
        // 读取黑名单的原始字符串列表（如 ["SHARPNESS", "PROTECTION"]）
        blacklistStr = config.getStringList("blacklist")
        // 将每个字符串通过 Paper 的注册表查找转换为 Enchantment 对象，
        // 转换结果（可能为 null）追加进 blackListEnchantments 列表
        blacklistStr.forEach {
            blackListEnchantments.add(getEnchantmentFromString(it))
        }
        // 读取 special-enchantment-setting 这一节（section），其结构形如：
        //   SHARPNESS:
        //     maximum-level: 5
        val specialEnchantmentsSection: ConfigurationSection? =
            config.getConfigurationSection("special-enchantment-setting")
        if (specialEnchantmentsSection != null) {
            var enchant: Enchantment
            var maximumLevels: Int
            // 遍历该 section 下的每个附魔键（如 "SHARPNESS"）
            for (str in specialEnchantmentsSection.getKeys(false)) {
                // 校验：附魔名称能查到、且该附魔下确实配置了 maximum-level 子键；
                // 任一条件不满足则跳过，避免后续出现空指针或非法值
                if (getEnchantmentFromString(str) == null ||
                    specialEnchantmentsSection.get("$str.maximum-level") == null
                ) continue
                // 通过校验后取出附魔对象与最大等级，组装成 SpecialEnchantments 存入映射表
                enchant = getEnchantmentFromString(str)!!
                maximumLevels = specialEnchantmentsSection.getInt("$str.maximum-level")
                specialEnchantments[enchant] = (SpecialEnchantments(enchant, maximumLevels))
            }
        }
        // 读取昂贵附魔提示消息（MiniMessage 格式），第二个参数为读取失败时的兜底默认值
        expensiveEnchantMessage = config.getString(
            "expensive-enchant-message",
            "<hover:show_item:enchanted_book></hover><RED>超出原版附魔显示，附魔所需等级为 <green>{level}</green>"
        )!!
        reloadMessage = config.getString("reload-message", "<green>重载配置文件完成.")!!
    }

    /**
     * 重载配置。
     *
     * 用于在服务器运行期间（例如管理员修改了 config.yml 后执行 `/unlockenchantment reload`）
     * 刷新内存中的配置字段。执行流程为：
     * 1. [initConfig] 补齐缺失的默认项并持久化；
     * 2. [config.load] 重新从磁盘读取最新的文件内容；
     * 3. [loadConfig] 将最新值映射到内存字段。
     */
    fun reloadConfig() {
        initConfig()
        config.load(configFile)
        loadConfig()
    }

    /**
     * 通过附魔的命名空间键字符串查找对应的 [Enchantment] 对象。
     *
     * 利用 Paper 提供的 [RegistryAccess] 获取附魔注册表，再用 [NamespacedKey.minecraft]
     * 将字符串构造为 `minecraft:xxx` 形式的键进行查找。
     *
     * @param str 附魔的命名空间键字符串，例如 `"sharpness"`、`"protection"`，
     *            大小写不敏感（最终会被统一处理为 minecraft 命名空间下的键）。
     * @return 查找到的 [Enchantment] 对象；若字符串无法匹配到任何已注册附魔，则返回 `null`。
     */
    fun getEnchantmentFromString(str: String): Enchantment? {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(NamespacedKey.minecraft(str))
    }
}

/**
 * 特殊附魔上限绑定数据类。
 *
 * 用于将一个 [Enchantment] 与其自定义的最大等级绑定在一起，
 * 对应配置文件 `special-enchantment-setting` 中每一项的解析结果。
 * 例如 `SHARPNESS -> maximum-level: 5` 会被解析为
 * `SpecialEnchantments(SHARPNESS, 5)`。
 *
 * @param enchant 该条设置所针对的附魔对象。
 * @param maximumLevels 为该附魔单独设置的最大等级上限。
 */
class SpecialEnchantments(val enchant: Enchantment, val maximumLevels: Int)
