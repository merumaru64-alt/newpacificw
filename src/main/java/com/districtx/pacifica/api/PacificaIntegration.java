package com.districtx.pacifica.api;

/** Callback used by integrations registered with Pacifica-II. */
public interface PacificaIntegration {
    default void onRegister(PacificaAPI api) { }

    default void onUnregister(PacificaAPI api) { }
}