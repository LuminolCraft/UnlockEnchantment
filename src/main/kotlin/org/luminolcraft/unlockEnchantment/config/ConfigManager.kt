package org.luminolcraft.unlockEnchantment.config

import com.google.common.collect.Lists
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.enchantments.Enchantment
import java.io.File

class ConfigManager(config: FileConfiguration) {
    val config: FileConfiguration = config
    val configFile: File = File(config.currentPath)

    var isPluginEnabled: Boolean = false
    var isEnchantmentSimplify:Boolean = false
    var maximumLevelCost: Int = -1
    private var blacklistStr: List<String> = Lists.newArrayList()
    var blackListEnchantments: MutableList<Enchantment?> = Lists.newArrayList()

    fun initConfig() {
        if (!configFile.exists()) {
            configFile.mkdirs()
            configFile.createNewFile()
            config.load(configFile)
            if (config.get("enabled") == null) {
                config.set("enabled", true)
                config.setComments("enabled", listOf("Set whether to enable the plugin's function"))
            }
            if (config.get("simplify-enchantment") == null) {
                config.set("simplify-enchantment", true)
                config.setComments("simplify-enchantment", listOf("Set whether to simplify the enchantment"))
            }
            if (config.get("maximum-level-cost") == null || config.getInt("maximum-level-cost") < -1) {
                config.set("maximum-level-cost", -1)
                config.setComments(
                    "maximum-level-cost",
                    listOf("Set the value of maximum level cost", "Set -1 to make the cost unlimited")
                )
            }
            if (config.get("blacklist") == null) {
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
                config.set("special-enchantment-setting", listOf(null))
                config.setComments(
                    "special-enchantment-setting", listOf(
                        "Set which enchantment should be specially adjusted",
                        "Example:",
                        "special-enchantment-setting:",
                        "  SHARPNESS:",
                        "    maximum-level-cost: 114",
                        "    maximum-level: 5"
                    )
                )
            }
            config.save(configFile)
        }
    }

    fun loadConfig() {
        initConfig()
        isPluginEnabled = config.getBoolean("enabled", true)
        isEnchantmentSimplify = config.getBoolean("simplify-enchantment", true)
        maximumLevelCost = config.getInt("maximum-level-cost", -1)
        blacklistStr = config.getStringList("blacklist")
        blacklistStr.forEach {
            blackListEnchantments.add(getEnchantmentFromString(it))
        }
    }

    fun reloadConfig() {
        initConfig()
        config.load(configFile)
        loadConfig()
    }

    fun getEnchantmentFromString(str: String): Enchantment? {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(NamespacedKey.minecraft(str))
    }
}