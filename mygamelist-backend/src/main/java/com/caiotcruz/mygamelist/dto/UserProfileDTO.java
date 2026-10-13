package com.caiotcruz.mygamelist.dto;

import jakarta.validation.constraints.Size;

public record UserProfileDTO(
    @Size(min = 2, max = 40) String name,
    @Size(max = 500) String bio,
    Boolean rotatingAvatar,
    @Size(max = 200000) String profilePicture
) {}