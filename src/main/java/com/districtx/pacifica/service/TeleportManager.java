package com.districtx.pacifica.service;

import com.districtx.pacifica.PacificaPlugin;
import com.districtx.pacifica.util.Items;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class TeleportManager {
    public enum TeleportContext { SPAWN, QUICK_PLAY, RANDOM_WARP, NAMED_WARP, PLAYER_TPA, HOUSE }

    private final PacificaPlugin plugin;
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();
    private final Map<UUID, BukkitTask> protectionTasks = new HashMap<>();

    public TeleportManager(PacificaPlugin plugin) { this.plugin = plugin; }

    public void start(Player player, Location destination, boolean guarded, Runnable success) {
        start(player, destination, guarded, null, java.util.Collections.emptyMap(), success);
    }

    public void start(Player player, Location destination, boolean guarded, TeleportContext context, Map<String, String> values) {
        start(player, destination, guarded, context, values, null);
    }

    private void start(Player player, Location destination, boolean guarded, TeleportContext context, Map<String, String> values, Runnable success) {
        if (plugin.combatTags().isTagged(player)) {
            player.sendMessage(plugin.store().message("combattag.teleport-blocked"));
            return;
        }
        if (destination == null || destination.getWorld() == null) {
            player.sendMessage(plugin.store().message("teleport.destination-unavailable"));
            return;
        }
        cancel(player, null);
        Location origin = player.getLocation().clone();
        boolean wasGuarded = plugin.statuses().isGuarded(player);
        int total = context == TeleportContext.QUICK_PLAY
            ? plugin.store().config().getInt("teleport.quick-play-countdown-seconds", 5)
            : plugin.store().config().getInt("teleport.countdown-seconds", 10);
        int effectDuration = plugin.store().config().getInt("teleport.final-effects-duration-seconds", 2) * 20;
        String sound = plugin.store().config().getString("teleport.sound", "UI_BUTTON_CLICK");
        BukkitTask task = new BukkitRunnable() {
            int elapsed;
            @Override public void run() {
                if (!player.isOnline()) { TeleportManager.this.cancel(player, null); return; }
                if (plugin.store().config().getBoolean("teleport.cancel-on-move", true) && moved(origin, player.getLocation())) {
                    TeleportManager.this.cancel(player, plugin.store().message("teleport.cancelled-move"));
                    return;
                }
                elapsed++;
                int remaining = total - elapsed;
                if (remaining == 1) {
                    Items.blindness(player, effectDuration);
                    Items.invisibility(player, effectDuration);
                }
                if (remaining > 0 && remaining <= 5) player.sendMessage(countdownMessage(context, values, remaining));
                if (remaining > 0 && plugin.store().config().getIntegerList("teleport.countdown-sound-thresholds").contains(remaining)) Items.clickSound(player, sound);
                if (remaining <= 0) {
                    tasks.remove(player.getUniqueId());
                    if (player.teleport(destination)) {
                        boolean protection = context != TeleportContext.SPAWN && (guarded || wasGuarded);
                        plugin.statuses().set(player, context == TeleportContext.SPAWN || protection);
                        scheduleProtectionExpiry(player, protection, context);
                        Items.clickSound(player, plugin.store().config().getString("teleport.success-sound", "BLOCK_FIRE_EXTINGUISH"));
                        player.sendMessage(successMessage(context, values));
                        if (success != null) success.run();
                    } else player.sendMessage(plugin.store().message("teleport.failed"));
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
        tasks.put(player.getUniqueId(), task);
        player.sendMessage(plugin.store().message("teleport.called"));
        if (total > 5) player.sendMessage(plugin.store().message("teleport.countdown-start", "%time%", String.valueOf(total)));
        else if (total > 0) player.sendMessage(countdownMessage(context, values, total));
        Items.clickSound(player, sound);
    }

    private void scheduleProtectionExpiry(Player player, boolean guarded, TeleportContext context) {
        BukkitTask previous = protectionTasks.remove(player.getUniqueId());
        if (previous != null) previous.cancel();
        if (!guarded || context == TeleportContext.SPAWN) return;
        long duration = Math.max(0L, plugin.store().config().getLong("spawn-protection.duration-seconds", 7L)) * 20L;
        if (duration == 0L) {
            plugin.statuses().set(player, false);
            return;
        }
        protectionTasks.put(player.getUniqueId(), new BukkitRunnable() {
            @Override public void run() {
                protectionTasks.remove(player.getUniqueId());
                if (player.isOnline()) plugin.statuses().set(player, false);
            }
        }.runTaskLater(plugin, duration));
    }

    private String successMessage(TeleportContext context, Map<String, String> values) {
        String path = switch (context == null ? TeleportContext.SPAWN : context) {
            case SPAWN -> "teleport.spawn-success";
            case QUICK_PLAY, RANDOM_WARP, NAMED_WARP -> "teleport.warp-success";
            case PLAYER_TPA -> "teleport.player-success";
            case HOUSE -> "teleport.house-success";
        };
        String message = plugin.store().message(context == null ? "teleport.success" : path);
        for (Map.Entry<String, String> entry : values.entrySet()) message = message.replace(entry.getKey(), entry.getValue());
        return message;
    }

    private String countdownMessage(TeleportContext context, Map<String, String> values, int remaining) {
        String path = context == TeleportContext.PLAYER_TPA ? "teleport.player-countdown" : "teleport.countdown";
        String message = plugin.store().message(path, "%time%", String.valueOf(remaining));
        for (Map.Entry<String, String> entry : values.entrySet()) message = message.replace(entry.getKey(), entry.getValue());
        return message;
    }

    private boolean moved(Location a, Location b) {
        return a.getWorld() != b.getWorld() || a.getBlockX() != b.getBlockX() || a.getBlockY() != b.getBlockY() || a.getBlockZ() != b.getBlockZ();
    }

    public void cancel(Player player, String message) {
        BukkitTask task = tasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
        if (message != null) player.sendMessage(message);
    }
    public void clear() {
        tasks.values().forEach(BukkitTask::cancel);
        protectionTasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
        protectionTasks.clear();
    }
}