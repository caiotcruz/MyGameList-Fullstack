package com.caiotcruz.mygamelist.dto;

import java.util.List;
import java.util.Map;

public record GameHubDTO(
    Long gameId,        
    Long externalId,    
    String title,
    String coverUrl,
    
    long totalPlayers,
    long playingCount,
    long completedCount,
    long platinumCount,
    Double communityScore, 
    Map<Integer, Long> scoreDistribution,
    
    String userStatus,    
    Integer userScore,     
    boolean isFavorite,

    List<GameReviewDTO> latestReviews
) {}