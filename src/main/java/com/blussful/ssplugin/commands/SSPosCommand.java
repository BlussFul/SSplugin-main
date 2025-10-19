package com.blussful.ssplugin.commands;

import com.blussful.ssplugin.managers.ConfigManager;
import com.blussful.ssplugin.SSPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class SSPosCommand implements CommandExecutor {

    private final SSPlugin plugin;
    private final ConfigManager config;

    public SSPosCommand(SSPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
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

        Location loc = player.getLocation();
        World world = loc.getWorld();

        if (world == null) {
            player.sendMessage(ChatColor.RED + "Ошибка: мир не найден.");
            return true;
        }

        // Проверка, что под игроком НЕ воздух
        Location blockBelow = loc.clone().subtract(0, 1, 0);
        if (blockBelow.getBlock().getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Нельзя ставить позицию проверки в воздухе! Поставьтесь на землю.");
            return true;
        }

        plugin.getConfig().set("checkLocation.world", world.getName());
        plugin.getConfig().set("checkLocation.x", loc.getX());
        plugin.getConfig().set("checkLocation.y", loc.getY());
        plugin.getConfig().set("checkLocation.z", loc.getZ());
        plugin.getConfig().set("checkLocation.yaw", loc.getYaw());
        plugin.getConfig().set("checkLocation.pitch", loc.getPitch());

        plugin.saveConfig();
        config.reloadConfigValues();

        player.sendMessage(ChatColor.GREEN + "Позиция для проверки успешно установлена!");

        return true;
    }
}
