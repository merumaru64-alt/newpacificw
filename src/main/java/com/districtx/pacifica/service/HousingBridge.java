package com.districtx.pacifica.service;

import com.districtx.pacifica.PacificaPlugin;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class HousingBridge {
    private static final String API_CLASS = "com.districtx.housing.api.PacificaHousingAPI";
    private static final String SERVICE_CLASS = "com.districtx.housing.api.PacificaHousingService";
    private final PacificaPlugin plugin;

    public HousingBridge(PacificaPlugin plugin) { this.plugin = plugin; }

    public HouseResult houses(Player player) {
        return houses(player.getUniqueId(), player);
    }

    public HouseResult houses(UUID playerId) {
        return houses(playerId, Bukkit.getPlayer(playerId));
    }

    private HouseResult houses(UUID playerId, Player player) {
        try {
            Plugin housing = plugin.getServer().getPluginManager().getPlugin("Pacifica-Housing");
            if (housing == null || !housing.isEnabled()) return unavailable();

            Class<?> apiType = loadClass(API_CLASS, housing);
            if (apiType == null) return unsupported();

            Object api = registeredService(apiType);
            if (api == null) api = singleton(apiType);
            if (api == null) api = pluginAccessor(housing, apiType);

            InvocationResult invocation = invokeOwnedHouses(api, playerId, player);
            if (!invocation.recognized()) {
                Class<?> serviceType = loadClass(SERVICE_CLASS, housing);
                Object service = serviceType == null ? null : registeredService(serviceType);
                invocation = invokeOwnedHouses(service, playerId, player);
            }
            if (!invocation.recognized() || invocation.value() == null) {
                return invocation.recognized() ? available(Collections.emptyList()) : unsupported();
            }

            List<HouseInfo> result = new ArrayList<>();
            collect(result, invocation.value(), playerId, Collections.newSetFromMap(new IdentityHashMap<>()));
            return available(result);
        } catch (RuntimeException | LinkageError ignored) {
            return unsupported();
        }
    }

    private HouseResult unavailable() {
        return new HouseResult(Availability.UNAVAILABLE, Collections.emptyList());
    }

    private HouseResult unsupported() {
        return new HouseResult(Availability.UNSUPPORTED, Collections.emptyList());
    }

    private HouseResult available(List<HouseInfo> houses) {
        return new HouseResult(Availability.AVAILABLE, Collections.unmodifiableList(houses));
    }

    private Class<?> loadClass(String name, Plugin housing) {
        try {
            return Class.forName(name, true, housing.getClass().getClassLoader());
        } catch (ClassNotFoundException | LinkageError ignored) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException | LinkageError ignoredAgain) {
                return null;
            }
        }
    }

    private Object registeredService(Class<?> type) {
        RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(type);
        return registration == null ? null : registration.getProvider();
    }

    private Object singleton(Class<?> type) {
        for (String name : new String[]{"getInstance", "instance"}) {
            try {
                Method method = type.getMethod(name);
                if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0) {
                    return method.invoke(null);
                }
            } catch (ReflectiveOperationException | IllegalArgumentException ignored) { }
        }
        return null;
    }

    private Object pluginAccessor(Plugin housing, Class<?> apiType) {
        for (String name : new String[]{"getAPI", "getApi", "getHousingAPI", "getHousingApi"}) {
            try {
                Method method = housing.getClass().getMethod(name);
                if (method.getParameterCount() == 0 && apiType.isAssignableFrom(method.getReturnType())) {
                    return method.invoke(housing);
                }
            } catch (ReflectiveOperationException | IllegalArgumentException ignored) { }
        }
        return null;
    }

    private InvocationResult invokeOwnedHouses(Object source, UUID playerId, Player player) {
        if (source == null) return new InvocationResult(false, null);
        String[] names = {"getOwnedHouses", "getPlayerOwnedHouses", "getHousesForPlayer", "getPlayerHouses", "getHouses"};
        for (String name : names) {
            for (Method method : source.getClass().getMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != 1) continue;
                Class<?> parameter = method.getParameterTypes()[0];
                Object argument = parameter == UUID.class ? playerId
                    : player != null && parameter.isInstance(player) ? player
                    : parameter == String.class ? playerId.toString() : null;
                if (argument == null) continue;
                try {
                    return new InvocationResult(true, method.invoke(source, argument));
                } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ignored) {
                    return new InvocationResult(true, null);
                }
            }
        }
        return new InvocationResult(false, null);
    }

    private void collect(List<HouseInfo> output, Object value, UUID playerId, Set<Object> visited) {
        if (value == null || !visited.add(value)) return;
        if (value instanceof Optional<?> optional) {
            optional.ifPresent(item -> collect(output, item, playerId, visited));
        } else if (value instanceof Map<?, ?> map) {
            Object owned = map.get(playerId);
            if (owned == null) owned = map.get(playerId.toString());
            if (owned != null) collect(output, owned, playerId, visited);
            else map.values().forEach(item -> collect(output, item, playerId, visited));
        } else if (value instanceof Collection<?> collection) {
            collection.forEach(item -> collect(output, item, playerId, visited));
        } else if (value instanceof Iterable<?> iterable) {
            iterable.forEach(item -> collect(output, item, playerId, visited));
        } else if (value.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(value); i++) collect(output, Array.get(value, i), playerId, visited);
        } else if (isHouse(value)) {
            add(output, value);
        } else {
            for (String name : new String[]{"getHouses", "getOwnedHouses", "getResults", "getContent", "getData"}) {
                Object nested = readValue(value, name);
                if (nested != null) {
                    collect(output, nested, playerId, visited);
                    return;
                }
            }
        }
    }

    private boolean isHouse(Object value) {
        String type = value.getClass().getSimpleName().toLowerCase();
        return type.contains("house") || readValue(value, "getName") != null
            || readValue(value, "name") != null || readValue(value, "getLocation") != null;
    }

    private void add(List<HouseInfo> output, Object house) {
        String name = readString(house, "getName", "getHouseName", "name", "houseName");
        if (name == null || name.isBlank()) return;
        String icon = readString(house, "getIcon", "getIconMaterial", "getMaterial", "icon", "iconMaterial", "material");
        String information = readInformation(house);
        output.add(new HouseInfo(name, information, icon == null ? "IRON_DOOR" : icon, readLocation(house, 0)));
    }

    private String readInformation(Object house) {
        List<String> values = new ArrayList<>();
        addText(values, readString(house, "getDescription", "getInformation", "getInfo", "description", "information", "info"));
        addText(values, label("Type", readString(house, "getType", "type", "getHouseType", "houseType")));
        addText(values, label("Status", readString(house, "getStatus", "status", "getHouseStatus", "houseStatus")));
        return String.join(" &7| ", values);
    }

    private String label(String label, String value) {
        return value == null || value.isBlank() ? null : "&7" + label + ": &f" + value;
    }

    private void addText(List<String> values, String value) {
        if (value != null && !value.isBlank()) values.add(value);
    }

    private Location readLocation(Object house, int depth) {
        if (depth > 3) return null;
        for (String name : new String[]{"getTeleportLocation", "getLocation", "getHomeLocation", "getTeleport", "getDoorInfo", "getDoor", "getHouseDoorInfo", "teleportLocation", "location"}) {
            Object value = readValue(house, name);
            Location location = toLocation(value);
            if (location != null) return location;
            if (value != null && value != house) {
                location = readLocation(value, depth + 1);
                if (location != null) return location;
            }
        }
        return toLocation(house);
    }

    private String readString(Object source, String... names) {
        for (String name : names) {
            Object value = readValue(source, name);
            if (value != null) return String.valueOf(value);
        }
        return null;
    }

    private Object readValue(Object source, String name) {
        if (source == null) return null;
        if (source instanceof Map<?, ?> map) {
            Object value = map.get(name);
            if (value == null && name.startsWith("get")) value = map.get(Character.toLowerCase(name.charAt(3)) + name.substring(4));
            if (value != null) return value;
        }
        try {
            return source.getClass().getMethod(name).invoke(source);
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private Location toLocation(Object value) {
        if (value instanceof Location location) return location;
        if (value == null) return null;
        Object worldValue = readValue(value, "getWorld");
        if (worldValue == null) worldValue = readValue(value, "getWorldName");
        if (worldValue == null) worldValue = readValue(value, "world");
        World world = worldValue instanceof World actual ? actual : worldValue == null ? null : Bukkit.getWorld(String.valueOf(worldValue));
        Number x = number(value, "getX", "x");
        Number y = number(value, "getY", "y");
        Number z = number(value, "getZ", "z");
        if (world == null || x == null || y == null || z == null) return null;
        Number yaw = number(value, "getYaw", "yaw");
        Number pitch = number(value, "getPitch", "pitch");
        return new Location(world, x.doubleValue(), y.doubleValue(), z.doubleValue(), yaw == null ? 0 : yaw.floatValue(), pitch == null ? 0 : pitch.floatValue());
    }

    private Number number(Object source, String... names) {
        for (String name : names) {
            Object value = readValue(source, name);
            if (value instanceof Number number) return number;
        }
        return null;
    }

    public enum Availability { UNAVAILABLE, UNSUPPORTED, AVAILABLE }
    public record HouseResult(Availability availability, List<HouseInfo> houses) { }
    private record InvocationResult(boolean recognized, Object value) { }
    public record HouseInfo(String name, String information, String icon, Location location) { }
}