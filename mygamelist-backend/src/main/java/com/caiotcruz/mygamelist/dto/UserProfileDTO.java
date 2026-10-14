package com.caiotcruz.mygamelist.dto;

import jakarta.validation.constraints.Size;

public record UserProfileDTO(
    @Size(min = 2, max = TextLimits.NAME) String name,
    @Size(max = TextLimits.BIO) String bio,
    Boolean rotatingAvatar,
    @Size(max = 200000) String profilePicture,
    com.caiotcruz.mygamelist.model.enums.ThemePreference theme
) {}
