package com.caiotcruz.mygamelist.dto.rawg;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RawgMovieDataDTO(
        @JsonProperty("480") String p480,
        String max
) {}