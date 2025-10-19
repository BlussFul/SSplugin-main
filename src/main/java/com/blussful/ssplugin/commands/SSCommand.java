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
import java.util.UUID;

public class SSCommand implements CommandExecutor, TabCompleter {

    private final CheckManager checkManager;
    private final ConfigManager config;

    public SSCommand(CheckManager checkManager, ConfigManager config) {
        this.checkManager = checkManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "[SSPlugin] " + ChatColor.WHITE + "Только игроки могут использовать эту команду");
            return true;
        }
        Player checker = (Player) sender;

        if (!checker.hasPermission("bl.ss")) {
            checker.sendMessage(config.getNoPermissionMessage());
            return true;
        }

        if (args.length == 0) {
            checker.sendMessage("");
            checker.sendMessage(ChatColor.DARK_GREEN + "               ssᴘʟᴜɢɪɴs               ");
            checker.sendMessage("");
            checker.sendMessage(ChatColor.GRAY + "  /ss " + ChatColor.DARK_GRAY + "<ᴘʟᴀʏᴇʀ> " + ChatColor.DARK_GRAY + "[-ᴀ]" + ChatColor.WHITE + " - вызвать/снять игрока с проверки");
            checker.sendMessage(ChatColor.GRAY + "  /ssstop " + ChatColor.DARK_GRAY + "<ᴘʟᴀʏᴇʀ>" + ChatColor.WHITE + " - остановить таймер проверки игрока");
            checker.sendMessage(ChatColor.GRAY + "  /sspos" + ChatColor.WHITE + " - установить кординаты для телепортации");
            checker.sendMessage(ChatColor.GRAY + "  /ssmsg" + ChatColor.WHITE + " - включитьь/выключить сообщения о проверках");
            checker.sendMessage(ChatColor.GRAY + "  /ssreload" + ChatColor.WHITE + " - перезагрузить конфигурацию плагина");
            checker.sendMessage("");
            checker.sendMessage(ChatColor.GRAY + "  Пример:" + ChatColor.WHITE + " /ss blussful -a");
            checker.sendMessage("");
            checker.sendMessage(ChatColor.GRAY + "  ᴘʟᴜɢɪɴ ᴄʀᴇᴀᴛᴇᴅ ʙʏ" + ChatColor.RED + " ʙʟᴜssꜰᴜʟ");
            checker.sendMessage("");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            checker.sendMessage(config.getPlayerNotFoundMessage());
            return true;
        }

        if (checker.getUniqueId().equals(target.getUniqueId())) {
            checker.sendMessage(ChatColor.RED + "Вы не можете поставить проверку на самого себя.");
            return true;
        }

        if (target.hasPermission("bl.ss.bypass")) {
            checker.sendMessage(config.getCannotCheckPlayerMessage());
            return true;
        }

        boolean teleport = args.length > 1 && args[1].equalsIgnoreCase("-a");

        UUID targetId = target.getUniqueId();

        if (checkManager.isPlayerChecked(targetId)) {
            checkManager.stopCheck(checker, target);

            checker.sendMessage(config.getToggleOffMessageChecker().replace("%target%", target.getName()));
            target.sendMessage(config.getToggleOffMessageTarget().replace("%checker%", checker.getName()));
        } else {
            checkManager.startCheck(checker, target, teleport);

            checker.sendMessage(config.getToggleOnMessageChecker().replace("%target%", target.getName()));
            target.sendMessage(config.getToggleOnMessageTarget().replace("%checker%", checker.getName()));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) {
                    matches.add(p.getName());
                }
            }
            return matches;
        }
        if (args.length == 2) {
            if ("-a".startsWith(args[1].toLowerCase())) {
                return Collections.singletonList("-a");
            }
        }
        return Collections.emptyList();
    }
}
