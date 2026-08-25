package com.districtx.pacifica.service;

import com.districtx.pacifica.PacificaPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class TpaManager {
    private final PacificaPlugin plugin;
    private final Map<UUID, Request> requests = new HashMap<>();
    public TpaManager(PacificaPlugin plugin) { this.plugin = plugin; }

    public void send(Player requester, Player target) {
        cancelOutgoing(requester, false);
        BukkitTask expiry = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Request request = requests.remove(requester.getUniqueId());
            if (request != null) {
                requester.sendMessage(plugin.store().message("tpa.expired-requester"));
                target.sendMessage(plugin.store().message("tpa.expired-target", "%player%", requester.getName()));
            }
        }, 600L);
        requests.put(requester.getUniqueId(), new Request(requester.getUniqueId(), target.getUniqueId(), expiry));
        requester.sendMessage(plugin.store().message("tpa.request-sent", "%player%", target.getName()));
        target.sendMessage(plugin.store().message("tpa.request-received", "%player%", requester.getName()));
    }

    public void accept(Player target) {
        Request request = findForTarget(target.getUniqueId());
        if (request == null) { target.sendMessage(plugin.store().message("tpa.no-pending")); return; }
        remove(request);
        Player requester = Bukkit.getPlayer(request.requester());
        if (requester == null) return;
        boolean guarded = plugin.statuses().isGuarded(target);
        plugin.teleports().start(requester, target.getLocation(), guarded, TeleportManager.TeleportContext.PLAYER_TPA, Collections.singletonMap("%player%", target.getName()));
        target.sendMessage(plugin.store().message("tpa.accepted"));
    }

    public void deny(Player target) {
        Request request = findForTarget(target.getUniqueId());
        if (request == null) { target.sendMessage(plugin.store().message("tpa.no-pending")); return; }
        remove(request);
        Player requester = Bukkit.getPlayer(request.requester());
        if (requester != null) requester.sendMessage(plugin.store().message("tpa.denied-requester"));
        target.sendMessage(plugin.store().message("tpa.denied"));
    }

    public void cancelOutgoing(Player requester, boolean notify) {
        Request request = requests.remove(requester.getUniqueId());
        if (request != null) {
            request.expiry().cancel();
            Player target = Bukkit.getPlayer(request.target());
            if (target != null) target.sendMessage(plugin.store().message("tpa.cancelled-target", "%player%", requester.getName()));
            if (notify) requester.sendMessage(plugin.store().message("tpa.cancelled"));
        }
    }

    public void playerQuit(Player player) {
        cancelOutgoing(player, false);
        Request incoming = findForTarget(player.getUniqueId());
        if (incoming != null) {
            remove(incoming);
            Player requester = Bukkit.getPlayer(incoming.requester());
            if (requester != null) requester.sendMessage(plugin.store().message("tpa.target-disconnected"));
        }
    }

    private Request findForTarget(UUID target) { return requests.values().stream().filter(request -> request.target().equals(target)).findFirst().orElse(null); }
    private void remove(Request request) { requests.remove(request.requester()); request.expiry().cancel(); }
    public void clear() { requests.values().forEach(request -> request.expiry().cancel()); requests.clear(); }
    private record Request(UUID requester, UUID target, BukkitTask expiry) { }
}