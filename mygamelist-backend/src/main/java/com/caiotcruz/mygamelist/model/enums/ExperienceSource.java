package com.caiotcruz.mygamelist.model.enums;

public enum ExperienceSource {
    
    GAME_ADDED(50),
    GAME_COMPLETED(200),
    GAME_PLATINUM(500),
    REVIEW_WRITTEN(50),

    LIKE_GIVEN(20),
    COMMENT_POSTED(10),
    
    USER_FOLLOWED(10),
    USER_GAINED_FOLLOWER(15);

    private final int amount;

    ExperienceSource(int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }
}