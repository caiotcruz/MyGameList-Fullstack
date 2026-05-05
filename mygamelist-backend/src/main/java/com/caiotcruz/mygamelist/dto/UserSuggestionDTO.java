package com.caiotcruz.mygamelist.dto;

import java.util.List;

public record UserSuggestionDTO(
    Long id,
    String name,
    String profilePicture,
    List<MutualFriendDTO> mutualFriends,
    long mutualCount
) {}