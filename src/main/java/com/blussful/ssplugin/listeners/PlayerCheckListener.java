package com.blussful.ssplugin.listeners;

import com.blussful.ssplugin.managers.CheckManager;
import com.blussful.ssplugin.managers.ConfigManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerCheckListener implements Listener {

    private final CheckManager checkManager;
    private final ConfigManager config;

    public PlayerCheckListener(CheckManager checkManager, ConfigManager config) {
        this.checkManager = checkManager;
        this.config = config;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (checkManager.isPlayerChecked(player.getUniqueId())) {
            if (event.getTo() != null && event.getFrom().distance(event.getTo()) > 0) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (checkManager.isPlayerChecked(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вы не можете взаимодействовать с блоками во время проверки.");
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        if (checkManager.isPlayerChecked(sender.getUniqueId())) {
            event.setCancelled(true);

            UUID checkerId = checkManager.getChecker(sender.getUniqueId());
            Player checker = Bukkit.getPlayer(checkerId);

            String originalMessage = event.getMessage();

            Pattern pattern = Pattern.compile("\\b(\\d[\\d ]{7,20}\\d)\\b");
            Matcher matcher = pattern.matcher(originalMessage);

            ComponentBuilder builder = new ComponentBuilder("[ss - " + sender.getName() + "] : ").color(ChatColor.GRAY);

            int lastIndex = 0;
            while (matcher.find()) {
                if (matcher.start() > lastIndex) {
                    builder.append(originalMessage.substring(lastIndex, matcher.start()));
                }

                String matched = matcher.group(1);
                String digitsOnly = matched.replace(" ", "");

                if (digitsOnly.length() >= 8 && digitsOnly.length() <= 11) {
                    TextComponent numberComponent = new TextComponent(matched);
                    numberComponent.setColor(ChatColor.GREEN);
                    numberComponent.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, digitsOnly));
                    numberComponent.setHoverEvent(createHoverEvent());

                    builder.append(numberComponent);
                } else {
                    builder.append(matched);
                }

                lastIndex = matcher.end();
            }

            if (lastIndex < originalMessage.length()) {
                builder.append(originalMessage.substring(lastIndex));
            }

            TextComponent finalMessage = new TextComponent(builder.create());

            Bukkit.getLogger().info("[ss - " + sender.getName() + "] : " + originalMessage);

            sender.spigot().sendMessage(finalMessage);

            if (checker != null) {
                checker.spigot().sendMessage(finalMessage);
            }

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.isOp()
                        && !onlinePlayer.getUniqueId().equals(sender.getUniqueId())
                        && (checker == null || !onlinePlayer.getUniqueId().equals(checker.getUniqueId()))
                        && !checkManager.isMessageToggledOff(onlinePlayer.getUniqueId())) {
                    onlinePlayer.spigot().sendMessage(finalMessage);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private HoverEvent createHoverEvent() {
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Нажмите, чтобы скопировать").color(ChatColor.YELLOW).create());
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (checkManager.isPlayerChecked(player.getUniqueId())) {
            String cmd = event.getMessage().split(" ")[0].toLowerCase();
            boolean allowed = config.getAllowedCommands().stream().anyMatch(c -> c.toLowerCase().equals(cmd));
            if (!allowed) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Вы не можете использовать эту команду во время проверки");
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!checkManager.isPlayerChecked(playerId)) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.setWalkSpeed(0.2f);

            BukkitRunnable task = checkManager.getTitleTasks().remove(playerId);
            if (task != null) task.cancel();

            BukkitRunnable timerTask = checkManager.getTimerTasks().remove(playerId);
            if (timerTask != null) timerTask.cancel();

            player.sendTitle("", "", 0, 0, 0);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (checkManager.isPlayerChecked(playerId)) {
            UUID checkerId = checkManager.getCheckingMap().remove(playerId);

            BukkitRunnable task = checkManager.getTitleTasks().remove(playerId);
            if (task != null) task.cancel();

            BukkitRunnable timerTask = checkManager.getTimerTasks().remove(playerId);
            if (timerTask != null) timerTask.cancel();

            checkManager.restorePlatform(player);

            if (checkerId != null) {
                Map<UUID, Integer> map = checkManager.getCheckerTimers().get(checkerId);
                if (map != null) {
                    map.remove(playerId);
                    if (map.isEmpty()) {
                        checkManager.getCheckerTimers().remove(checkerId);
                        BukkitRunnable task2 = checkManager.getCheckerActionBarTasks().remove(checkerId);
                        if (task2 != null) task2.cancel();
                    }
                }
            }

            CommandSender executor;
            if (checkerId != null) {
                Player checkerPlayer = Bukkit.getPlayer(checkerId);
                executor = (checkerPlayer != null) ? checkerPlayer : Bukkit.getConsoleSender();
            } else {
                executor = Bukkit.getConsoleSender();
            }

            checkManager.performAction(player, config.getActionOnQuit(), executor);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity damaged = event.getEntity();

        if (damager instanceof Player) {
            Player damagerPlayer = (Player) damager;
            if (checkManager.isPlayerChecked(damagerPlayer.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }

        if (damaged instanceof Player) {
            Player damagedPlayer = (Player) damaged;
            if (checkManager.isPlayerChecked(damagedPlayer.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (checkManager.isPlayerChecked(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (checkManager.isPlayerChecked(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вы не можете ломать блоки во время проверки.");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (checkManager.isPlayerChecked(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вы не можете ставить блоки во время проверки.");
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player)) return;

        Player player = (Player) event.getEntered();
        if (checkManager.isPlayerChecked(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вы не можете садиться на транспорт во время проверки.");
        }
    }
}
