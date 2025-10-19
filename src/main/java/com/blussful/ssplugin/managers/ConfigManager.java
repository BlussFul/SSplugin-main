package com.blussful.ssplugin.managers;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import com.blussful.ssplugin.SSPlugin;

import java.util.Collections;
import java.util.List;

public class ConfigManager {

    private final SSPlugin plugin;

    private List<String> allowedCommands;

    private String titleMain;
    private String titleSub;
    private String toggleOnMessageChecker;
    private String toggleOnMessageTarget;
    private String toggleOffMessageChecker;
    private String toggleOffMessageTarget;
    private String noPermissionMessage;
    private String playerNotFoundMessage;
    private String cannotCheckPlayerMessage;

    private int checkDurationSeconds;

    private Material platformBlockType;
    private boolean platformEnabled;

    private Location checkLocation;

    private String actionOnTimeout;
    private String actionOnQuit;

    public ConfigManager(SSPlugin plugin) {
        this.plugin = plugin;
    }

    public void reloadConfigValues() {
        plugin.reloadConfig();

        allowedCommands = plugin.getConfig().getStringList("command");
        if (allowedCommands.isEmpty()) allowedCommands = Collections.singletonList("/msg");

        titleMain = translate(plugin.getConfig().getString("messages.title_main", "&cВЫ НА ПРОВЕРКЕ"));
        titleSub = translate(plugin.getConfig().getString("messages.title_sub", "&eБудьте терпеливы и внимательны"));

        toggleOnMessageChecker = translate(plugin.getConfig().getString("messages.toggle_on_checker", "&aВы поставили %target% на проверку"));
        toggleOnMessageTarget = translate(plugin.getConfig().getString("messages.toggle_on_target", "&cВы поставлены на проверку игроком %checker%"));

        toggleOffMessageChecker = translate(plugin.getConfig().getString("messages.toggle_off_checker", "&aВы сняли проверку с %target%"));
        toggleOffMessageTarget = translate(plugin.getConfig().getString("messages.toggle_off_target", "&aПроверка с вас снята игроком %checker%"));

        noPermissionMessage = translate(plugin.getConfig().getString("messages.no_permission", "&cУ вас нет прав для использования этой команды"));
        playerNotFoundMessage = translate(plugin.getConfig().getString("messages.player_not_found", "&cИгрок не найден"));
        cannotCheckPlayerMessage = translate(plugin.getConfig().getString("messages.cannot_check_player", "&cВы не можете поставить на проверку этого игрока."));

        checkDurationSeconds = plugin.getConfig().getInt("check_duration_seconds", 180);

        String blockName = plugin.getConfig().getString("platform_block", "STONE");
        try {
            platformBlockType = Material.valueOf(blockName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Неверный тип блока платформы в конфиге: " + blockName + ", установлен STONE");
            platformBlockType = Material.STONE;
        }

        platformEnabled = plugin.getConfig().getBoolean("platform_enabled", true);

        actionOnTimeout = plugin.getConfig().getString("actions.on_timeout", "").trim();
        actionOnQuit = plugin.getConfig().getString("actions.on_quit", "").trim();

        loadCheckLocation();
    }

    private void loadCheckLocation() {
        String worldName = plugin.getConfig().getString("checkLocation.world");
        if (worldName != null && Bukkit.getWorld(worldName) != null) {
            double x = plugin.getConfig().getDouble("checkLocation.x");
            double y = plugin.getConfig().getDouble("checkLocation.y");
            double z = plugin.getConfig().getDouble("checkLocation.z");
            float yaw = (float) plugin.getConfig().getDouble("checkLocation.yaw");
            float pitch = (float) plugin.getConfig().getDouble("checkLocation.pitch");

            checkLocation = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
        } else {
            plugin.getLogger().warning("Мир " + worldName + " не найден, позиция проверки не загружена.");
            checkLocation = null;
        }
    }

    private String translate(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public List<String> getAllowedCommands() {
        return allowedCommands;
    }

    public String getTitleMain() {
        return titleMain;
    }

    public String getTitleSub() {
        return titleSub;
    }

    public String getToggleOnMessageChecker() {
        return toggleOnMessageChecker;
    }

    public String getToggleOnMessageTarget() {
        return toggleOnMessageTarget;
    }

    public String getToggleOffMessageChecker() {
        return toggleOffMessageChecker;
    }

    public String getToggleOffMessageTarget() {
        return toggleOffMessageTarget;
    }

    public String getNoPermissionMessage() {
        return noPermissionMessage;
    }

    public String getPlayerNotFoundMessage() {
        return playerNotFoundMessage;
    }

    public String getCannotCheckPlayerMessage() {
        return cannotCheckPlayerMessage;
    }

    public int getCheckDurationSeconds() {
        return checkDurationSeconds;
    }

    public Material getPlatformBlockType() {
        return platformBlockType;
    }

    // Исправленный метод без инверсии
    public boolean isPlatformEnabled() {
        return platformEnabled;
    }

    public Location getCheckLocation() {
        return checkLocation;
    }

    public String getActionOnTimeout() {
        return actionOnTimeout;
    }

    public String getActionOnQuit() {
        return actionOnQuit;
    }
}
