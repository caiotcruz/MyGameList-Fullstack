package com.caiotcruz.mygamelist.service;

import com.caiotcruz.mygamelist.model.Notification;
import com.caiotcruz.mygamelist.model.User;
import com.caiotcruz.mygamelist.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    @Test void cannotMarkAnotherUsersNotificationRead() {
        NotificationRepository repo = mock(NotificationRepository.class);
        NotificationService service = new NotificationService(repo, mock(CommunitySafety.class));
        User requester = new User();
        requester.setId(1L);
        when(repo.findByIdAndUser(99L, requester)).thenReturn(Optional.empty());
        service.markAsRead(99L, requester);
        verify(repo, never()).save(any(Notification.class));
        verify(repo, never()).findById(99L);
    }
}
