package com.blussful.ssplugin.commands;

import com.blussful.ssplugin.managers.CheckManager;
import com.blussful.ssplugin.managers.ConfigManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SSMsgToggleCommand implements CommandExecutor {

    private final CheckManager checkManager;
    private final ConfigManager config;

    public SSMsgToggleCommand(CheckManager checkManager, ConfigManager config) {
        this.checkManager = checkManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "[SSPlugin] Только игроки могут использовать эту команду");
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("bl.ss")) {
            player.sendMessage(config.getNoPermissionMessage());
            return true;
        }

        checkManager.toggleMessageForPlayer(player.getUniqueId());

        if (checkManager.isMessageToggledOff(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Сообщения о проверках отключены.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Сообщения о проверках включены.");
        }

        return true;
    }
}
