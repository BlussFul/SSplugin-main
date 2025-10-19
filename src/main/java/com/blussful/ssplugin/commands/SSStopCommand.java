package com.blussful.ssplugin.commands;

import com.blussful.ssplugin.managers.CheckManager;
import com.blussful.ssplugin.managers.ConfigManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SSStopCommand implements CommandExecutor, TabCompleter {

    private final CheckManager checkManager;
    private final ConfigManager config;

    public SSStopCommand(CheckManager checkManager, ConfigManager config) {
        this.checkManager = checkManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "[SSPlugin] Только игроки могут использовать эту команду");
            return true;
        }
        Player checker = (Player) sender;

        if (!checker.hasPermission("bl.ssstop")) {
            checker.sendMessage(config.getNoPermissionMessage());
            return true;
        }

        if (args.length == 0) {
            checker.sendMessage(ChatColor.RED + "Использование: /ssstop <игрок>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            checker.sendMessage(config.getPlayerNotFoundMessage());
            return true;
        }

        if (!checkManager.isPlayerChecked(target.getUniqueId())) {
            checker.sendMessage(ChatColor.RED + "Этот игрок не на проверке.");
            return true;
        }

        // Останавливаем таймер проверки, но не снимаем проверку
        checkManager.pauseCheckTimer(target);

        checker.sendMessage(ChatColor.YELLOW + "Таймер проверки для " + target.getName() + " приостановлен.");
        target.sendMessage(ChatColor.YELLOW + "Ваш таймер проверки был приостановлен игроком " + checker.getName() + ".");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix) && checkManager.isPlayerChecked(p.getUniqueId())) {
                    matches.add(p.getName());
                }
            }
            return matches;
        }
        return Collections.emptyList();
    }
}
