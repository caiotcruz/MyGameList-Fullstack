package com.caiotcruz.mygamelist.dto.rawg;

import java.util.List;

public record RawgAchievementsResponse(List<RawgAchievementDTO> results, String next) {}