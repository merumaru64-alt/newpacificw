package com.districtx.pacifica.service;

import com.districtx.pacifica.PacificaPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.metadata.FixedMetadataValue;

public final class CombatTagManager {
    private final PacificaPlugin plugin;
    private final NamespacedKey statusKey;
    private final Map<UUID, CombatState> states = new HashMap<>();

    public CombatTagManager(PacificaPlugin plugin) {
        this.plugin = plugin;
        statusKey = new NamespacedKey(plugin, "combattag.status");
    }

    public void tag(Player attacker, Player victim) {
        if (attacker.equals(victim)) return;
        int duration = Math.max(1, plugin.store().config().getInt("combattag.duration-seconds", 20));
        tagOne(attacker, victim, duration);
        tagOne(victim, attacker, duration);
    }

    private void tagOne(Player player, Player opponent, int duration) {
        CombatState state = states.get(player.getUniqueId());
        boolean alreadyTagged = state != null && state.task != null;
        if (state != null && state.task != null) state.task.cancel();
        CombatState next = new CombatState(opponent.getUniqueId(), duration, null);
        states.put(player.getUniqueId(), next);
        setStatus(player, true);
        if (!alreadyTagged) player.sendMessage(plugin.store().message("combattag.started", "%time%", String.valueOf(duration)));
        next.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> tick(player), 20L, 20L);
    }

    private void tick(Player player) {
        CombatState state = states.get(player.getUniqueId());
        if (state == null || !player.isOnline()) {
            if (state != null && state.task != null) state.task.cancel();
            states.remove(player.getUniqueId());
            return;
        }
        state.remaining--;
        if (state.remaining <= 0) {
            Player opponent = plugin.getServer().getPlayer(state.opponent);
            clear(player);
            if (opponent != null && isTagged(opponent)) clear(opponent);
            player.sendMessage(plugin.store().message("combattag.safe"));
            if (opponent != null && opponent.isOnline()) opponent.sendMessage(plugin.store().message("combattag.safe"));
            return;
        }
        if (plugin.store().config().getBoolean("combattag.send-countdown", true)) {
            player.sendMessage(plugin.store().message("combattag.countdown", "%time%", String.valueOf(state.remaining)));
        }
    }

    public boolean isTagged(Player player) {
        CombatState state = states.get(player.getUniqueId());
        return state != null && state.remaining > 0;
    }

    public int remaining(Player player) {
        CombatState state = states.get(player.getUniqueId());
        return state == null ? 0 : Math.max(0, state.remaining);
    }

    public Player opponent(Player player) {
        CombatState state = states.get(player.getUniqueId());
        return state == null ? null : plugin.getServer().getPlayer(state.opponent);
    }

    public void clear(Player player) {
        CombatState state = states.remove(player.getUniqueId());
        if (state != null && state.task != null) state.task.cancel();
        setStatus(player, false);
    }

    public void handleQuit(Player player) {
        if (!isTagged(player)) {
            clear(player);
            return;
        }
        Player opponent = opponent(player);
        if (opponent != null && opponent.isOnline()) {
            player.setHealth(0.0);
            clear(opponent);
            opponent.sendMessage(plugin.store().message("combattag.opponent-quit"));
        }
        clear(player);
    }

    public void handleDeath(Player victim, Player killer) {
        Player opponent = killer == null ? opponent(victim) : killer;
        clear(victim);
        if (opponent != null && opponent.isOnline() && isTagged(opponent)) clear(opponent);
        if (opponent != null && opponent.isOnline() && plugin.store().config().getBoolean("combattag.economy.enabled", true)) {
            double percentage = Math.max(0.0, plugin.store().config().getDouble("combattag.economy.victim-loss-percent", 20.0)) / 100.0;
            plugin.economy().transfer(opponent, victim, percentage);
        }
    }

    private void setStatus(Player player, boolean tagged) {
        String value = tagged ? "true" : "false";
        player.getPersistentDataContainer().set(statusKey, PersistentDataType.STRING, value);
        player.setMetadata("combattag.status", new FixedMetadataValue(plugin, value));
    }

    public boolean isAllowedCommand(String command) {
        String normalized = command.toLowerCase().split(" ", 2)[0];
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        for (String allowed : plugin.store().config().getStringList("combattag.allowed-commands")) {
            String value = allowed.toLowerCase().replaceFirst("^/", "");
            if (normalized.equals(value)) return true;
        }
        return normalized.equals("ct");
    }

    public void clear() {
        states.values().forEach(state -> { if (state.task != null) state.task.cancel(); });
        states.clear();
    }

    private static final class CombatState {
        private final UUID opponent;
        private int remaining;
        private BukkitTask task;

        private CombatState(UUID opponent, int remaining, BukkitTask task) {
            this.opponent = opponent;
            this.remaining = remaining;
            this.task = task;
        }
    }
}