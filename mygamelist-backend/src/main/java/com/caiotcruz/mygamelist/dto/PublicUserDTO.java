package com.caiotcruz.mygamelist.dto;
import com.caiotcruz.mygamelist.model.User;
public record PublicUserDTO(Long id, String name, String bio, String profilePicture,
        int level, int experience, boolean rotatingAvatar) {
    public static PublicUserDTO from(User u) {
        return new PublicUserDTO(u.getId(), u.getName(), u.getBio(), u.getProfilePicture(),
                u.getLevel(), u.getExperience(), u.isRotatingAvatar());
    }
}
