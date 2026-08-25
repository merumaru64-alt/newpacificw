package com.districtx.pacifica.service;

import com.districtx.pacifica.PacificaPlugin;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class WastedManager {
    private final PacificaPlugin plugin;
    private final Map<UUID, WastedState> states = new HashMap<>();

    public WastedManager(PacificaPlugin plugin) { this.plugin = plugin; }

    public void start(Player player, String type, Player killer) {
        cancel(player);
        Location deathLocation = player.getLocation().clone();
        player.setGameMode(GameMode.SPECTATOR);
        WastedState state = new WastedState(deathLocation, killer == null ? null : killer.getUniqueId(), null);
        states.put(player.getUniqueId(), state);
        String message = randomMessage(type);
        player.sendTitle(com.districtx.pacifica.util.Items.color("&c&lWasted"), com.districtx.pacifica.util.Items.color("&7" + message), 0, 100, 0);
        state.task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> finish(player), 100L);
        plugin.getServer().getScheduler().runTask(plugin, player.spigot()::respawn);
    }

    public void track(Player player) {
        if (isWasted(player)) player.setSpectatorTarget(null);
    }

    public boolean isWasted(Player player) { return states.containsKey(player.getUniqueId()); }

    public Location deathLocation(Player player) {
        WastedState state = states.get(player.getUniqueId());
        return state == null ? null : state.location.clone();
    }

    private void finish(Player player) {
        WastedState state = states.remove(player.getUniqueId());
        if (state == null || !player.isOnline()) return;
        Location spawn = plugin.store().spawn();
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(spawn == null ? player.getWorld().getSpawnLocation() : spawn);
        plugin.statuses().set(player, true);
        plugin.combatTags().clear(player);
    }

    private String randomMessage(String type) {
        List<String> values = plugin.store().wastedMessages().getStringList(type);
        if (values.isEmpty()) values = plugin.store().wastedMessages().getStringList("pvp");
        return values.isEmpty() ? "You were eliminated." : values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }

    public void cancel(Player player) {
        WastedState state = states.remove(player.getUniqueId());
        if (state != null && state.task != null) state.task.cancel();
    }

    public void clear() {
        states.values().forEach(state -> {
            if (state.task != null) state.task.cancel();
        });
        states.clear();
    }

    private static final class WastedState {
        private final Location location;
        private final UUID killer;
        private BukkitTask task;
        private WastedState(Location location, UUID killer, BukkitTask task) {
            this.location = location;
            this.killer = killer;
            this.task = task;
        }
    }
}