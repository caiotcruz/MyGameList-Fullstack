package com.caiotcruz.mygamelist.dto;

public record UserSummaryDTO(
    Long id,
    String name,
    boolean isFollowing 
) {
    public boolean getIsFollowing(){
        return this.isFollowing;
    }
}