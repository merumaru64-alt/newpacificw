package com.districtx.pacifica.api;

import com.districtx.pacifica.PacificaPlugin;
import com.districtx.pacifica.service.HousingBridge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PacificaApiProvider implements PacificaAPI {
    private final PacificaPlugin plugin;
    private final HousingBridge housing;
    private final Map<String, PacificaIntegration> integrations = new LinkedHashMap<>();
    private final Map<Class<?>, Object> services = new LinkedHashMap<>();

    public PacificaApiProvider(PacificaPlugin plugin, HousingBridge housing) {
        this.plugin = plugin;
        this.housing = housing;
    }

    @Override
    public String getApiVersion() {
        return "1";
    }

    @Override
    public List<PacificaHouse> getOwnedHouses(UUID playerId) {
        List<PacificaHouse> result = new ArrayList<>();
        housing.houses(playerId).houses().forEach(house -> result.add(
            new PacificaHouse(house.name(), house.information(), house.icon(), house.location())));
        return Collections.unmodifiableList(result);
    }

    @Override
    public boolean hasStatus(UUID playerId, String status) {
        return plugin.statuses().has(playerId, status);
    }

    @Override
    public DiamondCurrencyService diamonds() {
        return getService(DiamondCurrencyService.class).orElseThrow();
    }

    @Override
    public void registerIntegration(String id, PacificaIntegration integration) {
        if (id == null || id.isBlank() || integration == null) return;
        String key = id.toLowerCase(Locale.ROOT);
        PacificaIntegration previous = integrations.put(key, integration);
        if (previous != null) previous.onUnregister(this);
        integration.onRegister(this);
    }

    @Override
    public void unregisterIntegration(String id) {
        if (id == null) return;
        PacificaIntegration integration = integrations.remove(id.toLowerCase(Locale.ROOT));
        if (integration != null) integration.onUnregister(this);
    }

    @Override
    public <T> Optional<T> getService(Class<T> serviceType) {
        Object service = services.get(serviceType);
        return serviceType.isInstance(service) ? Optional.of(serviceType.cast(service)) : Optional.empty();
    }

    public <T> void registerService(Class<T> serviceType, T service) {
        if (serviceType != null && service != null) services.put(serviceType, service);
    }

    public void unregisterAll() {
        new ArrayList<>(integrations.keySet()).forEach(this::unregisterIntegration);
        services.clear();
    }
}