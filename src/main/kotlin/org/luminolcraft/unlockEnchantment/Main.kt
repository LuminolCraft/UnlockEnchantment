package org.luminolcraft.unlockEnchantment

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.luminolcraft.unlockEnchantment.config.ConfigManager


class Main : JavaPlugin() {

    override fun onEnable() {
        configManager = ConfigManager(config, this)
        configManager.loadConfig()
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
