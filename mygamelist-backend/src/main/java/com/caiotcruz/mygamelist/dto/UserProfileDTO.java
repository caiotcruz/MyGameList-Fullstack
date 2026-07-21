package com.caiotcruz.mygamelist.dto;

public record UserProfileDTO(
    String name,
    String bio,
    Boolean rotatingAvatar,
    String profilePicture
) {}