package com.caiotcruz.mygamelist.service;

import java.util.Map;

public class RawgStoreCatalog {
    private static final Map<Long, String> STORE_NAMES = Map.of(
            1L, "Steam",
            2L, "Xbox Store",
            3L, "PlayStation Store",
            4L, "App Store",
            5L, "GOG",
            6L, "Nintendo Store",
            7L, "Xbox 360 Store",
            8L, "Google Play",
            9L, "Epic Games"
    );

    public static String nameFor(Long storeId) {
        return STORE_NAMES.getOrDefault(storeId, "Loja desconhecida");
    }
}