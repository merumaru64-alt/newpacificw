package com.districtx.pacifica.gui;

import com.districtx.pacifica.PacificaPlugin;
import com.districtx.pacifica.service.HousingBridge.HouseInfo;
import com.districtx.pacifica.service.HousingBridge.HouseResult;
import com.districtx.pacifica.service.HousingBridge.Availability;
import com.districtx.pacifica.util.Items;
import com.districtx.pacifica.service.TeleportManager.TeleportContext;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class HouseMenu extends BaseMenu {
    private static final int[] CONTENT = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33, 38, 39, 40, 41, 42, 47, 48, 49, 50, 51};
    private List<HouseInfo> houses;
    public HouseMenu(PacificaPlugin plugin, Player player, int page) {
        super(plugin, player, "&7&lYour Houses", page);
        HouseResult result = plugin.housing().houses(player);
        houses = result.houses();
        if (result.availability() != Availability.AVAILABLE) {
            player.sendMessage(plugin.store().message("house.unavailable"));
            set(22, Items.create("BARRIER", plugin.store().message("house.unavailable")));
        } else if (houses.isEmpty()) {
            player.sendMessage(plugin.store().message("house.no-houses"));
            set(22, Items.create("BARRIER", plugin.store().message("house.no-houses")));
        }
        int start = page * CONTENT.length;
        for (int i = 0; i < CONTENT.length && start + i < houses.size(); i++) {
            HouseInfo house = houses.get(start + i);
            String info = house.information() == null ? "" : house.information();
            set(CONTENT[i], Items.create(house.icon(), "&f" + house.name(), info, "&7Click to teleport"));
        }
        set(45, Items.create("REDSTONE", "&c&lBack", "&7Return to the taxi page!"));
        if ((page + 1) * CONTENT.length < houses.size()) set(53, Items.create("ARROW", "&e&lNext Page", "&7Page " + (page + 2)));
    }
    @Override public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() != inventory) return;
        if (event.getSlot() == 45) { main(); return; }
        if (event.getSlot() == 53 && (page + 1) * CONTENT.length < houses.size()) { open(new HouseMenu(plugin, player, page + 1)); return; }
        HouseResult latest = plugin.housing().houses(player);
        if (latest.availability() != Availability.AVAILABLE) {
            player.sendMessage(plugin.store().message("house.unavailable"));
            return;
        }
        houses = latest.houses();
        for (int i = 0; i < CONTENT.length; i++) if (CONTENT[i] == event.getSlot() && page * CONTENT.length + i < houses.size()) {
            HouseInfo house = houses.get(page * CONTENT.length + i);
            if (house.location() == null || house.location().getWorld() == null) {
                player.sendMessage(plugin.store().message("house.invalid-location"));
                return;
            }
            plugin.teleports().start(player, house.location(), plugin.statuses().isGuarded(player), TeleportContext.HOUSE, Collections.singletonMap("%house%", house.name()));
            player.closeInventory();
            return;
        }
    }
}