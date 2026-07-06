package org.luminolcraft.unlockEnchantment

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.luminolcraft.unlockEnchantment.config.ConfigManager


//TODO 添加配置文件，实现加载插件却不开启附魔活动，以符合维护要求
class Main : JavaPlugin() {

    override fun onEnable() {
        configManager = ConfigManager(config)
        Bukkit.getPluginManager().registerEvents(AnvilListener(), this)
        logger.info("Loaded")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
    companion object {
        lateinit var configManager: ConfigManager
    }
}
