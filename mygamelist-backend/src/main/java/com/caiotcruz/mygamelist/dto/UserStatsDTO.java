package com.caiotcruz.mygamelist.dto;

import java.util.List;

public record UserStatsDTO(
        long followersCount,
        long followingCount,
        List<ActivityUserDTO> followers,
        List<ActivityUserDTO> following
) {}