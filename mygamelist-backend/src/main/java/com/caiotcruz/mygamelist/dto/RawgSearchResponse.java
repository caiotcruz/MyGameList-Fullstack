package com.caiotcruz.mygamelist.dto;

import java.util.List;

public record RawgSearchResponse(
    Integer count,
    List<GameResultDTO> results 
) {}