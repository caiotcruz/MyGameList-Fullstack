package com.caiotcruz.mygamelist.dto;
import com.caiotcruz.mygamelist.model.User;
public record PublicUserDTO(Long id, String name, String bio, String profilePicture,
        int level, int experience, boolean rotatingAvatar, com.caiotcruz.mygamelist.model.enums.ThemePreference theme) {
    public static PublicUserDTO from(User u) {
        return new PublicUserDTO(u.getId(), u.getName(), u.getBio(), u.getProfilePicture(),
                u.getLevel(), u.getExperience(), u.isRotatingAvatar(), u.getTheme());
    }
}
