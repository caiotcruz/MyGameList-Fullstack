package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.dto.FeedPageDTO;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.ActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class FeedService {
    private final CommunitySafety safety;

    private static final int PAGE_SIZE = 20;
    private final ActivityRepository activities;
    private final ActivityGroupingService grouping;
    public FeedService(ActivityRepository activities, ActivityGroupingService grouping, CommunitySafety safety) {
        this.safety = safety;
        this.activities = activities;
        this.grouping = grouping;
    }

    @Transactional(readOnly = true)
    public FeedPageDTO page(User user, Long before) {
        if (before != null && before <= 0) throw new IllegalArgumentException("Cursor inválido");
        var candidates = activities.findFeedGroups(user.getId(), before == null ? Long.MAX_VALUE : before, PAGE_SIZE + 1);
        if (candidates.isEmpty()) return new FeedPageDTO(List.of(), null, false);
        boolean hasMore = candidates.size() > PAGE_SIZE;
        var selected = candidates.subList(0, Math.min(PAGE_SIZE, candidates.size()));
        var keys = selected.stream().map(ActivityRepository.FeedGroup::getGroupKey).toList();
        var events = activities.findVisibleGroupEvents(user.getId(), keys);
        var hidden=safety.hiddenIds(user.getId());
        var items = grouping.groupAndMap(events, user.getId()).stream().map(a -> new com.caiotcruz.mygamelist.dto.GroupedActivityDTO(a.id(),a.timestamp(),a.user(),a.game(),a.types(),a.statusDetail(),a.ratingValue(),a.reviewText(),a.reviewSpoiler(),a.likesCount(),a.likedByMe(),a.comments().stream().filter(c -> !hidden.contains(c.user().id())).toList())).toList();
        Long nextCursor = hasMore ? selected.get(selected.size() - 1).getAnchorId() : null;
        return new FeedPageDTO(items, nextCursor, hasMore);
    }
}
