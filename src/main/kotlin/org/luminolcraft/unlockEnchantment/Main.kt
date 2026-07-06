package org.luminolcraft.unlockEnchantment

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin


//TODO 添加配置文件，实现加载插件却不开启附魔活动，以符合维护要求
class Main : JavaPlugin() {

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(AnvilListener(), this)
        logger.info("Loaded")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
