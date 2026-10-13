package com.caiotcruz.mygamelist.dto;
import java.util.List;
public record FeedPageDTO(List<GroupedActivityDTO> items, Long nextCursor, boolean hasMore) {}
