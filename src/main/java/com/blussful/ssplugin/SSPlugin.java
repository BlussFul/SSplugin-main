package com.blussful.ssplugin;

import com.blussful.ssplugin.commands.*;
import com.blussful.ssplugin.listeners.PlayerCheckListener;
import com.blussful.ssplugin.managers.CheckManager;
import com.blussful.ssplugin.managers.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class SSPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ConfigManager configManager = new ConfigManager(this);
        configManager.reloadConfigValues();

        CheckManager checkManager = new CheckManager(this, configManager);

        Objects.requireNonNull(getCommand("ss")).setExecutor(new SSCommand(checkManager, configManager));
        Objects.requireNonNull(getCommand("ss")).setTabCompleter(new SSCommand(checkManager, configManager));

        Objects.requireNonNull(getCommand("ssstop")).setExecutor(new SSStopCommand(checkManager, configManager));
        Objects.requireNonNull(getCommand("ssstop")).setTabCompleter(new SSStopCommand(checkManager, configManager));

        Objects.requireNonNull(getCommand("sspos")).setExecutor(new SSPosCommand(this, configManager));

        Objects.requireNonNull(getCommand("ssmsg")).setExecutor(new SSMsgToggleCommand(checkManager, configManager));

        Objects.requireNonNull(getCommand("ssreload")).setExecutor(new SSReloadCommand(this, configManager));

        getServer().getPluginManager().registerEvents(new PlayerCheckListener(checkManager, configManager), this);

        getLogger().info("[SSPlugin] плагин включен!");
    }

}
