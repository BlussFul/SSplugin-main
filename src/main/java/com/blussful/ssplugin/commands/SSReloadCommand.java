package com.blussful.ssplugin.commands;

import com.blussful.ssplugin.managers.ConfigManager;
import com.blussful.ssplugin.SSPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SSReloadCommand implements CommandExecutor {

    private final SSPlugin plugin;
    private final ConfigManager config;

    public SSReloadCommand(SSPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bl.ss.reload")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав для перезагрузки плагина.");
            return true;
        }

        plugin.reloadConfig();
        config.reloadConfigValues();

        sender.sendMessage(ChatColor.GREEN + "Конфигурация плагина успешно перезагружена.");

        return true;
    }
}
