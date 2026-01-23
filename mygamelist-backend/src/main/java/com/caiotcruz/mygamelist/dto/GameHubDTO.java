package com.caiotcruz.mygamelist.dto;

import java.util.List;

public record GameHubDTO(
    Long gameId,        
    Long externalId,    
    String title,
    String coverUrl,
    
    long totalPlayers,
    long playingCount,
    long completedCount,
    Double communityScore, 
    
    String userStatus,    
    Integer userScore,     
    boolean isFavorite,

    List<GameReviewDTO> latestReviews
) {}