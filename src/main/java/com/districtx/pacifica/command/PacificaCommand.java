package com.districtx.pacifica.command;

import com.districtx.pacifica.PacificaPlugin;
import com.districtx.pacifica.gui.TaxiMenu;
import com.districtx.pacifica.gui.TrashMenu;
import com.districtx.pacifica.service.TeleportManager.TeleportContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

public final class PacificaCommand implements CommandExecutor, TabCompleter {
    private final PacificaPlugin plugin;
    public PacificaCommand(PacificaPlugin plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("spawn")) { return spawn(sender); }
        if (name.equals("warp")) { return warp(sender); }
        if (name.equals("setspawn")) { if (!admin(sender) || !(sender instanceof Player player)) return true; plugin.store().setSpawn(player.getLocation()); sender.sendMessage(plugin.store().message("admin.spawn-set")); return true; }
        if (name.equals("rndmwarpset")) { if (!admin(sender) || !(sender instanceof Player player) || args.length != 1) return true; plugin.warps().set(args[0], player.getLocation()); sender.sendMessage(plugin.store().message("admin.random-warp-set", "%warp%", args[0])); return true; }
        if (name.equals("taxi")) return taxi(sender, args);
        if (name.equals("trashcan")) return trash(sender, args);
        if (name.equals("tpa")) return tpa(sender, args);
        if (name.equals("ct")) return combatTag(sender);
        if (name.equals("diamonds")) return diamonds(sender);
        if (name.equals("admindias")) return adminDiamonds(sender, args);
        return true;
    }
    private boolean spawn(CommandSender sender) { if (!(sender instanceof Player player)) return true; Location location = plugin.store().spawn(); plugin.teleports().start(player, location == null ? player.getWorld().getSpawnLocation() : location, true, TeleportContext.SPAWN, Collections.emptyMap()); return true; }
    private boolean warp(CommandSender sender) { if (!(sender instanceof Player player)) return true; String name = plugin.warps().randomName(); if (name == null) player.sendMessage(plugin.store().message("warp.no-random")); else plugin.teleports().start(player, plugin.warps().get(name), false, TeleportContext.RANDOM_WARP, Collections.singletonMap("%warp%", name)); return true; }
    private boolean taxi(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length == 1 && args[0].equalsIgnoreCase("link")) {
            if (!admin(sender)) return true;
            Entity entity = player.getTargetEntity(8);
            if (entity == null) player.sendMessage(plugin.store().message("admin.look-at-npc")); else { plugin.store().setTaxiNpc(entity.getUniqueId()); player.sendMessage(plugin.store().message("admin.taxi-linked")); }
        } else plugin.guis().open(new TaxiMenu(plugin, player));
        return true;
    }
    private boolean trash(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length == 1 && args[0].equalsIgnoreCase("set")) {
            if (!admin(sender)) return true;
            org.bukkit.block.Block block = player.getTargetBlockExact(6);
            if (block == null) player.sendMessage(plugin.store().message("admin.look-at-block")); else { plugin.store().addTrashCan(block.getLocation()); plugin.holograms().create(block.getLocation()); player.sendMessage(plugin.store().message("admin.trash-linked")); }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("unlink")) {
            if (!admin(sender)) return true;
            org.bukkit.block.Block block = player.getTargetBlockExact(6);
            if (block == null || !plugin.store().isTrashCan(block.getLocation())) player.sendMessage(plugin.store().message("admin.trash-not-linked"));
            else { plugin.holograms().remove(block.getLocation()); plugin.store().removeTrashCan(block.getLocation()); player.sendMessage(plugin.store().message("admin.trash-unlinked")); }
        } else plugin.guis().open(new TrashMenu(plugin, player));
        return true;
    }
    private boolean tpa(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) return true;
        switch (args[0].toLowerCase(Locale.ROOT)) { case "accept" -> plugin.tpa().accept(player); case "deny" -> plugin.tpa().deny(player); case "cancel" -> plugin.tpa().cancelOutgoing(player, true); default -> player.sendMessage(plugin.store().message("tpa.usage")); }
        return true;
    }
    private boolean combatTag(CommandSender sender) {
        if (!(sender instanceof Player player)) return true;
        if (plugin.combatTags().isTagged(player)) player.sendMessage(plugin.store().message("combattag.status", "%time%", String.valueOf(plugin.combatTags().remaining(player))));
        else player.sendMessage(plugin.store().message("combattag.not-in-combat"));
        return true;
    }
    private boolean diamonds(CommandSender sender) {
        if (!(sender instanceof Player player)) return true;
        player.sendMessage(plugin.store().message("diamonds.balance", "%diamonds%", String.valueOf(plugin.diamonds().balance(player.getUniqueId()))));
        return true;
    }
    private boolean adminDiamonds(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pacifica.admindias")) {
            sender.sendMessage(plugin.store().message("admin.no-permission"));
            return true;
        }
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage(plugin.store().message("diamonds.usage"));
            return true;
        }
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[1]);
        if (target.getName() == null) {
            sender.sendMessage(plugin.store().message("diamonds.player-not-found"));
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("clear") || action.equals("reset")) {
            if (args.length != 2) { sender.sendMessage(plugin.store().message("diamonds.usage")); return true; }
            plugin.diamonds().clear(target.getUniqueId());
            sender.sendMessage(plugin.store().message("diamonds.updated", "%player%", target.getName(), "%diamonds%", "0"));
            return true;
        }
        if (!action.equals("set") && !action.equals("give")) { sender.sendMessage(plugin.store().message("diamonds.usage")); return true; }
        if (args.length != 3) { sender.sendMessage(plugin.store().message("diamonds.usage")); return true; }
        long amount;
        try { amount = Long.parseLong(args[2]); } catch (NumberFormatException exception) { sender.sendMessage(plugin.store().message("diamonds.invalid-amount")); return true; }
        if (amount < 0 || (action.equals("give") && amount == 0)) { sender.sendMessage(plugin.store().message("diamonds.invalid-amount")); return true; }
        if (action.equals("set")) plugin.diamonds().set(target.getUniqueId(), amount); else plugin.diamonds().give(target.getUniqueId(), amount);
        sender.sendMessage(plugin.store().message("diamonds.updated", "%player%", target.getName(), "%diamonds%", String.valueOf(plugin.diamonds().balance(target.getUniqueId()))));
        return true;
    }
    private boolean admin(CommandSender sender) { if (!sender.hasPermission("pacifica.admin")) sender.sendMessage(plugin.store().message("admin.no-permission")); return sender.hasPermission("pacifica.admin"); }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("tpa") && args.length == 1) return Arrays.asList("accept", "deny", "cancel");
        if (command.getName().equalsIgnoreCase("rndmwarpset") && args.length == 1) return plugin.warps().names();
        if (command.getName().equalsIgnoreCase("admindias") && sender.hasPermission("pacifica.admindias")) {
            if (args.length == 1) return Arrays.asList("set", "give", "clear", "reset");
            if (args.length == 2) return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}