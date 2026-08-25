package com.districtx.pacifica.api;

import org.bukkit.Location;

/** Immutable house data exposed by Pacifica-II integrations. */
public record PacificaHouse(String name, String information, String icon, Location location) { }