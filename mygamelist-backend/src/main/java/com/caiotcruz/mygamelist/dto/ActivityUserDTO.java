package com.caiotcruz.mygamelist.dto;

import com.caiotcruz.mygamelist.model.User;

public record ActivityUserDTO(Long id, String name, String profilePicture) {
    public static ActivityUserDTO from(User user) {
        return new ActivityUserDTO(
            user.getId(), 
            user.getName(), 
            user.getProfilePicture()
        );
    }
}