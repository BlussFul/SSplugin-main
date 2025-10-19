package com.blussful.ssplugin.managers;

import com.blussful.ssplugin.SSPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CheckManager {
    private final SSPlugin plugin;
    private final ConfigManager config;

    private final Map<UUID, UUID> checkingMap = new HashMap<>();
    private final Map<UUID, BukkitRunnable> titleTasks = new HashMap<>();
    private final Map<UUID, BukkitRunnable> timerTasks = new HashMap<>();
    private final Map<UUID, Map<UUID, Integer>> checkerTimers = new HashMap<>();
    private final Map<UUID, BukkitRunnable> checkerActionBarTasks = new HashMap<>();
    private final Map<UUID, Map<Location, Material>> originalBlocks = new HashMap<>();
    private final Set<UUID> opMessageToggleOff = new HashSet<>();

    // Множество игроков, которые сейчас притягиваются вниз
    private final Set<UUID> pullingDownPlayers = new HashSet<>();

    public CheckManager(SSPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean isPlayerChecked(UUID playerId) {
        return checkingMap.containsKey(playerId);
    }

    public UUID getChecker(UUID targetId) {
        return checkingMap.get(targetId);
    }

    public void toggleMessageForPlayer(UUID playerId) {
        if (opMessageToggleOff.contains(playerId)) {
            opMessageToggleOff.remove(playerId);
        } else {
            opMessageToggleOff.add(playerId);
        }
    }

    public boolean isMessageToggledOff(UUID playerId) {
        return opMessageToggleOff.contains(playerId);
    }

    public void startCheck(Player checker, Player target, boolean teleportToCheckLocation) {
        UUID targetId = target.getUniqueId();

        checkingMap.put(targetId, checker.getUniqueId());

        target.setWalkSpeed(0f);
        target.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));

        Location loc;

        if (teleportToCheckLocation) {
            if (config.getCheckLocation() != null) {
                loc = roundLocationY(config.getCheckLocation());
            } else {
                loc = getSafeLocationBelow(target);
            }
        } else {
            loc = getSafeLocationBelow(target);
        }

        target.teleport(loc);

        if (!teleportToCheckLocation) {
            if (config.isPlatformEnabled()) {
                buildPlatform(target, loc);
            } else {
                // Плавно притягиваем игрока вниз до Y = -70
                pullPlayerDown(target, -70);
            }
        }

        if (teleportToCheckLocation && config.getCheckLocation() != null) {
            Location checkerLoc = config.getCheckLocation().clone().add(1, 0, 0);
            checker.teleport(checkerLoc);
        }

        target.sendTitle(config.getTitleMain(), config.getTitleSub(), 10, 70, 20);

        BukkitRunnable titleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!checkingMap.containsKey(target.getUniqueId())) {
                    this.cancel();
                    titleTasks.remove(target.getUniqueId());
                    return;
                }
                target.sendTitle(config.getTitleMain(), config.getTitleSub(), 10, 70, 20);
            }
        };
        titleTask.runTaskTimer(plugin, 0L, 60L);
        titleTasks.put(targetId, titleTask);

        Map<UUID, Integer> map = checkerTimers.computeIfAbsent(checker.getUniqueId(), k -> new HashMap<>());
        map.put(targetId, config.getCheckDurationSeconds());

        startCheckerActionBarTask(checker);

        createAndStartTimerTask(target, checker);
    }

    public void stopCheck(Player checker, Player target) {
        UUID targetId = target.getUniqueId();

        checkingMap.remove(targetId);

        target.setWalkSpeed(0.2f);
        target.removePotionEffect(PotionEffectType.BLINDNESS);

        BukkitRunnable titleTask = titleTasks.remove(targetId);
        if (titleTask != null) titleTask.cancel();

        BukkitRunnable timerTask = timerTasks.remove(targetId);
        if (timerTask != null) timerTask.cancel();

        target.sendTitle("", "", 0, 0, 0);
        target.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));

        restorePlatform(target);

        // Если игрок притягивался вниз, останавливаем это
        UUID playerId = target.getUniqueId();
        if (pullingDownPlayers.contains(playerId)) {
            pullingDownPlayers.remove(playerId);
        }

        UUID checkerId = checker.getUniqueId();
        Map<UUID, Integer> map = checkerTimers.get(checkerId);
        if (map != null) {
            map.remove(targetId);
            if (map.isEmpty()) {
                checkerTimers.remove(checkerId);
                BukkitRunnable task = checkerActionBarTasks.remove(checkerId);
                if (task != null) task.cancel();
            }
        }
    }

    // Приостановить таймер проверки, не снимая проверку
    public void pauseCheckTimer(Player target) {
        UUID targetId = target.getUniqueId();

        BukkitRunnable timerTask = timerTasks.remove(targetId);
        if (timerTask != null) {
            timerTask.cancel();
        }
    }

    private Location roundLocationY(Location loc) {
        return new Location(loc.getWorld(), loc.getX(), Math.floor(loc.getY()), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    // Метод для получения стартовой безопасной локации (без притягивания)
    private Location getSafeLocationBelow(Player player) {
        Location loc = player.getLocation().clone();
        World world = loc.getWorld();
        if (world == null) return loc;

        if (!config.isPlatformEnabled()) {
            // При выключенной платформе просто возвращаем текущую позицию без изменений,
            // т.к. потом игрок будет плавно опускаться методом pullPlayerDown
            return loc;
        }

        // Если платформа включена, используем стандартную логику поиска безопасного места
        int y = loc.getBlockY();
        while (y > 0) {
            Location checkLoc = new Location(world, loc.getX(), y, loc.getZ());
            if (!checkLoc.getBlock().getType().isAir()) {
                return new Location(world, loc.getX(), y + 1.0, loc.getZ(), loc.getYaw(), loc.getPitch());
            }
            y--;
        }
        return roundLocationY(loc);
    }

    private void buildPlatform(Player player, Location centerLoc) {
        if (!config.isPlatformEnabled()) return;

        UUID playerId = player.getUniqueId();
        Map<Location, Material> savedBlocks = originalBlocks.computeIfAbsent(playerId, k -> new HashMap<>());

        Location base = centerLoc.clone().subtract(0, 1, 0);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Location blockLoc = base.clone().add(dx, 0, dz);
                Block block = blockLoc.getBlock();

                savedBlocks.putIfAbsent(blockLoc, block.getType());

                block.setType(config.getPlatformBlockType());
            }
        }
    }

    public void restorePlatform(Player player) {
        if (!config.isPlatformEnabled()) return;

        UUID playerId = player.getUniqueId();
        Map<Location, Material> savedBlocks = originalBlocks.remove(playerId);

        if (savedBlocks != null) {
            for (Map.Entry<Location, Material> entry : savedBlocks.entrySet()) {
                Location loc = entry.getKey();
                Material originalMat = entry.getValue();

                Block block = loc.getBlock();
                if (block.getType() == config.getPlatformBlockType() || !block.getType().equals(originalMat)) {
                    block.setType(originalMat);
                }
            }
        }
    }

    private void startCheckerActionBarTask(Player checker) {
        UUID checkerId = checker.getUniqueId();
        if (checkerActionBarTasks.containsKey(checkerId)) return;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                Map<UUID, Integer> map = checkerTimers.get(checkerId);
                if (map == null || map.isEmpty()) {
                    checker.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
                    this.cancel();
                    checkerActionBarTasks.remove(checkerId);
                    return;
                }

                StringBuilder sb = new StringBuilder();
                for (Map.Entry<UUID, Integer> entry : map.entrySet()) {
                    Player target = Bukkit.getPlayer(entry.getKey());
                    if (target != null) {
                        int time = entry.getValue();
                        int minutes = time / 60;
                        int seconds = time % 60;
                        sb.append(target.getName())
                                .append(" - ")
                                .append(String.format("%02d:%02d", minutes, seconds))
                                .append(" | ");
                    }
                }
                if (sb.length() >= 3) {
                    sb.setLength(sb.length() - 3);
                }
                String message = sb.toString();
                checker.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + message));
            }
        };
        task.runTaskTimer(plugin, 0L, 20L);
        checkerActionBarTasks.put(checkerId, task);
    }

    private void createAndStartTimerTask(Player target, Player checker) {
        final Player targetFinal = target;
        final Player checkerFinal = checker;
        final String actionOnTimeoutFinal = config.getActionOnTimeout();
        final int duration = config.getCheckDurationSeconds();

        BukkitRunnable timerTask = new BukkitRunnable() {
            int timeLeft = duration;

            @Override
            public void run() {
                if (!checkingMap.containsKey(targetFinal.getUniqueId())) {
                    targetFinal.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
                    this.cancel();
                    timerTasks.remove(targetFinal.getUniqueId());

                    Map<UUID, Integer> map = checkerTimers.get(checkerFinal.getUniqueId());
                    if (map != null) {
                        map.remove(targetFinal.getUniqueId());
                        if (map.isEmpty()) {
                            checkerTimers.remove(checkerFinal.getUniqueId());
                            BukkitRunnable task = checkerActionBarTasks.remove(checkerFinal.getUniqueId());
                            if (task != null) task.cancel();
                        }
                    }
                    return;
                }
                if (timeLeft <= 0) {
                    checkingMap.remove(targetFinal.getUniqueId());

                    targetFinal.setWalkSpeed(0.2f);
                    targetFinal.removePotionEffect(PotionEffectType.BLINDNESS);
                    targetFinal.sendTitle("", "", 0, 0, 0);
                    targetFinal.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));

                    BukkitRunnable tTask = titleTasks.remove(targetFinal.getUniqueId());
                    if (tTask != null) tTask.cancel();

                    restorePlatform(targetFinal);

                    UUID playerId = targetFinal.getUniqueId();
                    if (pullingDownPlayers.contains(playerId)) {
                        pullingDownPlayers.remove(playerId);
                    }

                    Map<UUID, Integer> map = checkerTimers.get(checkerFinal.getUniqueId());
                    if (map != null) {
                        map.remove(targetFinal.getUniqueId());
                        if (map.isEmpty()) {
                            checkerTimers.remove(checkerFinal.getUniqueId());
                            BukkitRunnable task = checkerActionBarTasks.remove(checkerFinal.getUniqueId());
                            if (task != null) task.cancel();
                        }
                    }

                    CommandSender executor = Bukkit.getPlayer(checkerFinal.getUniqueId());
                    if (executor == null) executor = Bukkit.getConsoleSender();

                    performAction(targetFinal, actionOnTimeoutFinal, executor);

                    this.cancel();
                    timerTasks.remove(targetFinal.getUniqueId());
                    return;
                }
                int minutes = timeLeft / 60;
                int seconds = timeLeft % 60;
                String timeStr = String.format("%02d:%02d", minutes, seconds);
                targetFinal.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.RED + "Время на проверку: " + ChatColor.YELLOW + timeStr));

                Map<UUID, Integer> map = checkerTimers.get(checkerFinal.getUniqueId());
                if (map != null) {
                    map.put(targetFinal.getUniqueId(), timeLeft);
                }

                timeLeft--;
            }
        };
        timerTask.runTaskTimer(plugin, 0L, 20L);
        timerTasks.put(target.getUniqueId(), timerTask);
    }

    public void performAction(Player target, String commandLine, CommandSender executor) {
        if (commandLine == null || commandLine.isEmpty()) return;

        String cmd = commandLine.replace("%user%", target.getName());
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }

        plugin.getLogger().info("Выполнение команды: " + cmd + " от " + executor.getName());

        String finalCmd = cmd;
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(executor, finalCmd));
    }

    // Метод плавного опускания игрока вниз до targetY
    public void pullPlayerDown(Player player, double targetY) {
        UUID playerId = player.getUniqueId();

        if (pullingDownPlayers.contains(playerId)) return;

        pullingDownPlayers.add(playerId);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    pullingDownPlayers.remove(playerId);
                    this.cancel();
                    return;
                }

                Location loc = player.getLocation();
                if (loc.getY() <= targetY) {
                    pullingDownPlayers.remove(playerId);
                    this.cancel();
                    return;
                }

                Location below = loc.clone().subtract(0, 0.2, 0);
                Block blockBelow = below.getBlock();

                if (isPassable(blockBelow.getType())) {
                    loc.setY(loc.getY() - 0.2);
                    player.teleport(loc);
                } else {
                    pullingDownPlayers.remove(playerId);
                    this.cancel();
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 1L); // запуск каждую 1 тик (50ms)
    }


    // Проверка проходимости блока (воздух, вода, лава и т.п.)
    private boolean isPassable(Material material) {
        if (material.isAir()) return true;

        switch (material) {
            case WATER:
            case LAVA:
            case CAVE_AIR:
            case VOID_AIR:
            case FIRE:
                return true;
            default:
                return false;
        }
    }

    public Map<UUID, UUID> getCheckingMap() {
        return checkingMap;
    }

    public Map<UUID, BukkitRunnable> getTitleTasks() {
        return titleTasks;
    }

    public Map<UUID, BukkitRunnable> getTimerTasks() {
        return timerTasks;
    }

    public Map<UUID, BukkitRunnable> getCheckerActionBarTasks() {
        return checkerActionBarTasks;
    }

    public Map<UUID, Map<UUID, Integer>> getCheckerTimers() {
        return checkerTimers;
    }
}
