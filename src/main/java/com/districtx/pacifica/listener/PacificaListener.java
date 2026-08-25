package com.districtx.pacifica.listener;

import com.districtx.pacifica.PacificaPlugin;
import com.districtx.pacifica.gui.TaxiMenu;
import com.districtx.pacifica.gui.TrashMenu;
import com.districtx.pacifica.util.Items;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.EventPriority;

public final class PacificaListener implements Listener {
    private final PacificaPlugin plugin;
    public PacificaListener(PacificaPlugin plugin) { this.plugin = plugin; }
    @EventHandler public void join(PlayerJoinEvent event) {
        Player player = event.getPlayer(); plugin.statuses().set(player, true);
        Bukkit.getScheduler().runTask(plugin, () -> { if (!player.isOnline()) return; org.bukkit.Location spawn = plugin.store().spawn(); player.teleport(spawn == null ? player.getWorld().getSpawnLocation() : spawn); });
    }
    @EventHandler public void respawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        org.bukkit.Location wastedLocation = plugin.wasted().deathLocation(player);
        if (wastedLocation != null) {
            event.setRespawnLocation(wastedLocation);
            Bukkit.getScheduler().runTask(plugin, () -> { if (player.isOnline() && plugin.wasted().isWasted(player)) { player.setGameMode(org.bukkit.GameMode.SPECTATOR); plugin.wasted().track(player); } });
            return;
        }
        org.bukkit.Location spawn = plugin.store().spawn();
        if (spawn != null) event.setRespawnLocation(spawn);
        plugin.statuses().set(player, true);
    }
    @EventHandler public void quit(PlayerQuitEvent event) { plugin.teleports().cancel(event.getPlayer(), null); plugin.tpa().playerQuit(event.getPlayer()); }
    @EventHandler public void taxi(PlayerInteractEntityEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Entity entity = event.getRightClicked(); String plain = entity.getCustomName() == null ? "" : ChatColor.stripColor(entity.getCustomName());
        if (entity.getUniqueId().equals(plugin.store().taxiNpc()) || plain.equalsIgnoreCase("Taxi")) { event.setCancelled(true); plugin.guis().open(new TaxiMenu(plugin, player)); }
    }
    @EventHandler public void trash(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || !plugin.store().isTrashCan(event.getClickedBlock().getLocation())) return;
        event.setCancelled(true); plugin.guis().open(new TrashMenu(plugin, event.getPlayer()));
    }
    @EventHandler(priority = EventPriority.LOW)
    public void death(PlayerDeathEvent event) {
        Player player = event.getEntity();
        EntityDamageEvent.DamageCause cause = player.getLastDamageCause() == null ? null : player.getLastDamageCause().getCause();
        String type = player.getKiller() != null ? "pvp" : cause == EntityDamageEvent.DamageCause.STARVATION ? "starvation" : cause == EntityDamageEvent.DamageCause.FALL ? "fall" : "pvp";
        event.setDeathMessage(null);
        plugin.wasted().start(player, type, player.getKiller());
    }
    @EventHandler
    public void move(PlayerMoveEvent event) {
        if (!plugin.wasted().isWasted(event.getPlayer())) return;
        if (event.getTo() != null && (event.getTo().getX() != event.getFrom().getX() || event.getTo().getY() != event.getFrom().getY() || event.getTo().getZ() != event.getFrom().getZ() || event.getTo().getYaw() != event.getFrom().getYaw() || event.getTo().getPitch() != event.getFrom().getPitch())) event.setTo(event.getFrom());
        plugin.wasted().track(event.getPlayer());
    }
    @EventHandler public void wastedSneak(PlayerToggleSneakEvent event) { if (plugin.wasted().isWasted(event.getPlayer())) event.setCancelled(true); }
    @EventHandler public void wastedFlight(PlayerToggleFlightEvent event) { if (plugin.wasted().isWasted(event.getPlayer())) event.setCancelled(true); }
    @EventHandler
    public void wastedTeleport(PlayerTeleportEvent event) {
        if (plugin.wasted().isWasted(event.getPlayer())) event.setCancelled(true);
    }
}