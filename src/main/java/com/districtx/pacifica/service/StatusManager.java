package com.districtx.pacifica.service;

import com.districtx.pacifica.PacificaPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import java.util.UUID;

public final class StatusManager {
    private final PacificaPlugin plugin;
    private final NamespacedKey key;

    public StatusManager(PacificaPlugin plugin) { this.plugin = plugin; key = new NamespacedKey(plugin, "spawn.guard"); }
    public void set(Player player, boolean guarded) {
        String value = guarded ? "true" : "false";
        player.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        player.setMetadata("spawn.guard", new FixedMetadataValue(plugin, value));
    }
    public boolean isGuarded(Player player) {
        String value = player.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return value == null || Boolean.parseBoolean(value);
    }
    public boolean has(UUID playerId, String status) {
        Player player = plugin.getServer().getPlayer(playerId);
        return player != null && status.equals("spawn.guard." + (isGuarded(player) ? "true" : "false"));
    }
}