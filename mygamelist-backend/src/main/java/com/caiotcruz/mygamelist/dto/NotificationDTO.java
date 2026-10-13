package com.caiotcruz.mygamelist.dto;
import com.caiotcruz.mygamelist.model.Notification;
import com.caiotcruz.mygamelist.model.enums.NotificationType;
import java.time.LocalDateTime;
public record NotificationDTO(Long id, PublicUserDTO actor, NotificationType type,
        Long activityId, boolean read, LocalDateTime createdAt) {
    public static NotificationDTO from(Notification n) {
        return new NotificationDTO(n.getId(), PublicUserDTO.from(n.getActor()), n.getType(),
                n.getActivity() == null ? null : n.getActivity().getId(), n.isRead(), n.getCreatedAt());
    }
}
