package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.model.*;
import com.caiotcruz.mygamelist.model.enums.ExperienceSource;
import com.caiotcruz.mygamelist.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SocialServiceTest {
    private final CommentRepository comments = mock(CommentRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final LevelService levels = mock(LevelService.class);
    private final SocialService service = new SocialService(mock(ActivityLikeRepository.class), comments,
            mock(ActivityRepository.class), users, mock(NotificationService.class), levels, mock(UserLocks.class), mock(CommunitySafety.class));
    private User owner;
    private User author;
    private Comment comment;

    @BeforeEach void setup() {
        owner = user(1L, "owner@example.test");
        author = user(2L, "author@example.test");
        Activity activity = new Activity(); activity.setId(10L); activity.setUser(owner);
        comment = new Comment("Uma opinião", author, activity); comment.setId(100L);
        when(comments.existsById(100L)).thenReturn(true);
        when(comments.findById(100L)).thenReturn(Optional.of(comment));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private User user(long id, String email) {
        User user = new User(); user.setId(id); user.setEmail(email); return user;
    }
    private void login(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(users.findByEmail(user.getEmail())).thenReturn(user);
    }
    @Test void ownerRemovingOthersCommentRevokesAuthorsReward() {
        login(owner);
        service.removeComment(10L, 100L);
        verify(comments).delete(comment);
        verify(levels).revoke(author, ExperienceSource.COMMENT_POSTED);
        verify(levels, never()).revoke(owner, ExperienceSource.COMMENT_POSTED);
    }
    @Test void ownActivityCommentDoesNotRevokeXpThatWasNeverGranted() {
        comment.setUser(owner);
        login(owner);
        service.removeComment(10L,100L);
        verify(comments).delete(comment);
        verifyNoInteractions(levels);
    }
    @Test void unrelatedUserCannotDelete() {
        login(user(3L, "other@example.test"));
        assertThrows(RuntimeException.class, () -> service.removeComment(10L,100L));
        verify(comments, never()).delete(any(Comment.class));
        verifyNoInteractions(levels);
    }
    @Test void mismatchedActivityCannotDelete() {
        login(owner);
        assertThrows(IllegalArgumentException.class, () -> service.removeComment(11L,100L));
        verify(comments, never()).delete(any(Comment.class));
        verifyNoInteractions(levels);
    }
}
