package com.caiotcruz.mygamelist.dto;

import com.caiotcruz.mygamelist.model.Game;

public record ActivityGameDTO(Long id, Long rawgId, String title, String coverUrl) {
    public static ActivityGameDTO from(Game game) {
        return new ActivityGameDTO(
            game.getId(), 
            game.getRawgId(), 
            game.getTitle(), 
            game.getCoverUrl()
        );
    }
}