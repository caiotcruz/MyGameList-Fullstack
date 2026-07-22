package com.caiotcruz.mygamelist.dto;

public record UserSummaryDTO(
    Long id,
    String name,
    boolean isFollowing,
    String profilePicture,
    boolean rotatingAvatar
) {
    public boolean getIsFollowing(){
        return this.isFollowing;
    }
}