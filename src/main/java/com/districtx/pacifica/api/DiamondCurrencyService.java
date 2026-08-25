package com.districtx.pacifica.api;

import java.util.UUID;

/** Public Diamond Currency operations available to integrations. */
public interface DiamondCurrencyService {
    long balance(UUID playerId);

    void set(UUID playerId, long amount);

    void give(UUID playerId, long amount);

    void clear(UUID playerId);
}