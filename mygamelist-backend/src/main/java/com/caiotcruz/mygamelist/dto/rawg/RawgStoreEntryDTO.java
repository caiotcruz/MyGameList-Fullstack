package com.caiotcruz.mygamelist.dto.rawg;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RawgStoreEntryDTO(
        Long id,
        String url,
        @JsonProperty("store_id") Long storeId
) {}