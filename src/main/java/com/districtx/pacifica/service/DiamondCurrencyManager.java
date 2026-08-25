package com.districtx.pacifica.service;

import com.districtx.pacifica.PacificaPlugin;
import com.districtx.pacifica.api.DiamondCurrencyService;
import java.util.UUID;

public final class DiamondCurrencyManager implements DiamondCurrencyService {
    private final PacificaPlugin plugin;

    public DiamondCurrencyManager(PacificaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public long balance(UUID playerId) {
        return Math.max(0L, plugin.store().data().getLong(path(playerId), 0L));
    }

    @Override public void set(UUID playerId, long amount) {
        plugin.store().data().set(path(playerId), Math.max(0L, amount));
        plugin.store().save();
    }

    @Override public void give(UUID playerId, long amount) {
        if (amount <= 0) return;
        long current = balance(playerId);
        set(playerId, current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount);
    }

    @Override public void clear(UUID playerId) {
        set(playerId, 0L);
    }

    private String path(UUID playerId) {
        return "diamonds." + playerId;
    }
}