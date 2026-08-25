package com.districtx.pacifica.gui;

import com.districtx.pacifica.PacificaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.configuration.ConfigurationSection;
import com.districtx.pacifica.service.TeleportManager.TeleportContext;
import java.util.Collections;

public final class TaxiMenu extends BaseMenu {
    public TaxiMenu(PacificaPlugin plugin, Player player) {
        super(plugin, player, string(plugin, "title", "&7&lTaxi"), 0);
        ConfigurationSection section = plugin.store().config().getConfigurationSection("taxi-gui");
        mainButton(integer(section, "buttons.spawn.slot", 13), string(section, "buttons.spawn.material", "BED"), string(section, "buttons.spawn.name", "&a&lSpawn"), string(section, "buttons.spawn.lore", "&7Teleport to spawn!"));
        mainButton(integer(section, "buttons.quick-play.slot", 11), string(section, "buttons.quick-play.material", "EMERALD"), string(section, "buttons.quick-play.name", "&a&lQuick Play"), string(section, "buttons.quick-play.lore", "&7Teleport to a random location!"));
        mainButton(integer(section, "buttons.warps.slot", 31), string(section, "buttons.warps.material", "ENDER_PEARL"), string(section, "buttons.warps.name", "&e&lWarps"), string(section, "buttons.warps.lore", "&7Browse the warp list!"));
        mainButton(integer(section, "buttons.player.slot", 29), string(section, "buttons.player.material", "PLAYER_HEAD"), string(section, "buttons.player.name", "&e&lPlayer"), string(section, "buttons.player.lore", "&7Send a teleport request"));
        mainButton(integer(section, "buttons.house.slot", 15), string(section, "buttons.house.material", "IRON_DOOR"), string(section, "buttons.house.name", "&1House"), string(section, "buttons.house.lore", "&7Travel to one of your houses!"));
    }

    @Override public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() != inventory || event.getCurrentItem() == null) return;
        int slot = event.getSlot();
        if (slot == buttonSlot("spawn", 13)) {
                plugin.teleports().start(player, plugin.store().spawn() == null ? player.getWorld().getSpawnLocation() : plugin.store().spawn(), true, TeleportContext.SPAWN, Collections.emptyMap());
                player.closeInventory();
        } else if (slot == buttonSlot("quick-play", 11)) {
                String name = plugin.warps().randomName();
                if (name == null) { player.sendMessage(plugin.store().message("warp.no-random")); return; }
                plugin.teleports().start(player, plugin.warps().get(name), false, TeleportContext.QUICK_PLAY, Collections.singletonMap("%warp%", name));
                player.closeInventory();
        } else if (slot == buttonSlot("warps", 31)) {
            open(new WarpMenu(plugin, player, 0));
        } else if (slot == buttonSlot("player", 29)) {
            open(new PlayerMenu(plugin, player, 0));
        } else if (slot == buttonSlot("house", 15)) {
            open(new HouseMenu(plugin, player, 0));
        }
    }

    private static String string(PacificaPlugin plugin, String path, String fallback) {
        ConfigurationSection section = plugin.store().config().getConfigurationSection("taxi-gui");
        return string(section, path, fallback);
    }
    private static String string(ConfigurationSection section, String path, String fallback) { return section == null ? fallback : section.getString(path, fallback); }
    private static int integer(ConfigurationSection section, String path, int fallback) { return section == null ? fallback : section.getInt(path, fallback); }
    private int buttonSlot(String button, int fallback) {
        ConfigurationSection section = plugin.store().config().getConfigurationSection("taxi-gui");
        return integer(section, "buttons." + button + ".slot", fallback);
    }
}