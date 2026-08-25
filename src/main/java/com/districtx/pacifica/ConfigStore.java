package com.districtx.pacifica;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigStore {
    private final PacificaPlugin plugin;
    private final File folder;
    private final FileConfiguration config;
    private final FileConfiguration prices;
    private final FileConfiguration messages;
    private final FileConfiguration wastedMessages;
    private final FileConfiguration data;
    private final File dataFile;
    private final HashMap<UUID, String> lastChat = new HashMap<>();

    public ConfigStore(PacificaPlugin plugin) {
        this.plugin = plugin;
        File parent = plugin.getDataFolder().getParentFile();
        folder = new File(parent == null ? plugin.getDataFolder() : parent, "Pacifica-II");
        if (!folder.exists() && !folder.mkdirs()) throw new IllegalStateException("Could not create Pacifica folder");
        copyDefault("config.yml");
        copyDefault("item_prices.yml");
        copyDefault("messages.yml");
        copyDefault("wasted_Messages.yml");
        config = YamlConfiguration.loadConfiguration(new File(folder, "config.yml"));
        prices = YamlConfiguration.loadConfiguration(new File(folder, "item_prices.yml"));
        messages = YamlConfiguration.loadConfiguration(new File(folder, "messages.yml"));
        wastedMessages = YamlConfiguration.loadConfiguration(new File(folder, "wasted_Messages.yml"));
        dataFile = new File(folder, "data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void copyDefault(String name) {
        File file = new File(folder, name);
        if (file.exists()) return;
        try (InputStream in = plugin.getResource(name)) {
            if (in != null) Files.copy(in, file.toPath());
            else if (!file.createNewFile()) throw new IOException("Could not create " + name);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create " + name, exception);
        }
    }

    public FileConfiguration config() { return config; }
    public FileConfiguration prices() { return prices; }
    public FileConfiguration messages() { return messages; }
    public FileConfiguration wastedMessages() { return wastedMessages; }
    public FileConfiguration data() { return data; }
    public String lastChat(org.bukkit.entity.Player player) { return lastChat.getOrDefault(player.getUniqueId(), ""); }
    public void setLastChat(org.bukkit.entity.Player player, String message) { lastChat.put(player.getUniqueId(), message); }
    public String message(String path, String... replacements) {
        String value = messages.getString(path, path);
        for (int i = 0; i + 1 < replacements.length; i += 2) value = value.replace(replacements[i], replacements[i + 1]);
        return com.districtx.pacifica.util.Items.color(value);
    }
    public void save() {
        try { data.save(dataFile); }
        catch (IOException exception) { plugin.getLogger().warning("Could not save Pacifica data: " + exception.getMessage()); }
    }

    public Location spawn() {
        return location(data, "spawn");
    }

    public void setSpawn(Location location) {
        setLocation(data, "spawn", location);
        save();
    }

    public List<String> warpNames() {
        ConfigurationSection section = data.getConfigurationSection("warps");
        return section == null ? Collections.emptyList() : new ArrayList<>(section.getKeys(false));
    }

    public Location warp(String name) { return location(data, "warps." + name); }

    public void setWarp(String name, Location location) {
        setLocation(data, "warps." + name, location);
        save();
    }

    public UUID taxiNpc() {
        String value = data.getString("taxi-npc");
        try { return value == null ? null : UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    public void setTaxiNpc(UUID uuid) { data.set("taxi-npc", uuid.toString()); save(); }

    public boolean isTrashCan(Location location) {
        for (String key : data.getStringList("trash-cans")) {
            if (key.equals(locationKey(location))) return true;
        }
        return false;
    }

    public void addTrashCan(Location location) {
        List<String> locations = new ArrayList<>(data.getStringList("trash-cans"));
        String key = locationKey(location);
        if (!locations.contains(key)) locations.add(key);
        data.set("trash-cans", locations);
        save();
    }

    public void removeTrashCan(Location location) {
        List<String> locations = new ArrayList<>(data.getStringList("trash-cans"));
        if (locations.remove(locationKey(location))) {
            data.set("trash-cans", locations);
            save();
        }
    }

    public List<Location> trashCanLocations() {
        List<Location> locations = new ArrayList<>();
        for (String key : data.getStringList("trash-cans")) {
            String[] parts = key.split(":", 4);
            if (parts.length != 4) continue;
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) continue;
            try { locations.add(new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]))); }
            catch (NumberFormatException ignored) { }
        }
        return locations;
    }

    public List<String> holograms() { return data.getStringList("holograms"); }
    public void setHolograms(List<String> ids) { data.set("holograms", ids); save(); }
    public List<String> trashHolograms(Location location) { return data.getStringList("trash-holograms." + hologramKey(location)); }
    public void setTrashHolograms(Location location, List<String> ids) { data.set("trash-holograms." + hologramKey(location), ids); save(); }
    public void removeTrashHolograms(Location location) { data.set("trash-holograms." + hologramKey(location), null); save(); }

    private static String locationKey(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private static String hologramKey(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private static Location location(FileConfiguration source, String path) {
        ConfigurationSection section = source.getConfigurationSection(path);
        if (section == null) return null;
        World world = Bukkit.getWorld(section.getString("world", ""));
        return world == null ? null : new Location(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"), (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
    }

    private static void setLocation(FileConfiguration target, String path, Location location) {
        target.set(path + ".world", location.getWorld().getName());
        target.set(path + ".x", location.getX());
        target.set(path + ".y", location.getY());
        target.set(path + ".z", location.getZ());
        target.set(path + ".yaw", location.getYaw());
        target.set(path + ".pitch", location.getPitch());
    }
}