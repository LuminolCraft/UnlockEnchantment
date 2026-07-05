package org.luminolcraft.unlockEnchantment

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(AnvilListener(), this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
