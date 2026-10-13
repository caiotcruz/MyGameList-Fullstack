package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.model.UserGameList;
import com.caiotcruz.mygamelist.model.enums.GameStatus;
import com.caiotcruz.mygamelist.repository.UserFollowRepository;
import com.caiotcruz.mygamelist.repository.UserGameListRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AchievementServiceTest {
    @Test void progressIsBoundedAndUnlockedOnlyAtThreshold() {
        UserGameListRepository games = mock(UserGameListRepository.class);
        UserFollowRepository follows = mock(UserFollowRepository.class);
        User user = new User();
        user.setId(4L);
        UserGameList first = new UserGameList();
        first.setStatus(GameStatus.PLATINUM);
        first.setReview("Bom jogo");
        when(games.findByUserId(4L)).thenReturn(List.of(first));
        var achievements = new AchievementService(games, follows).forUser(user);
        assertTrue(achievements.stream().filter(a -> a.id().equals("first-platinum"))
                .findFirst().orElseThrow().unlocked());
        var collector = achievements.stream().filter(a -> a.id().equals("collector-10"))
                .findFirst().orElseThrow();
        assertEquals(1, collector.progress());
        assertFalse(collector.unlocked());
    }
}
