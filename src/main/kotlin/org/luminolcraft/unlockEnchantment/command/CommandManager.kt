package org.luminolcraft.unlockEnchantment.command

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.luminolcraft.unlockEnchantment.Main

class CommandManager : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        Main.configManager.reloadConfig()
        sender.sendMessage(MiniMessage.miniMessage().deserialize(Main.configManager.reloadMessage))
        return true
    }
}