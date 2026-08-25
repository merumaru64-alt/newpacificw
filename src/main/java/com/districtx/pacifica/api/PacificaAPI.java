package com.districtx.pacifica.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public integration point for Pacifica-II.
 *
 * <p>Other plugins should obtain this service from Bukkit's service manager
 * rather than depending on Pacifica-II implementation classes.</p>
 */
public interface PacificaAPI {
    String getApiVersion();

    List<PacificaHouse> getOwnedHouses(UUID playerId);

    boolean hasStatus(UUID playerId, String status);

    DiamondCurrencyService diamonds();

    void registerIntegration(String id, PacificaIntegration integration);

    void unregisterIntegration(String id);

    <T> Optional<T> getService(Class<T> serviceType);
}