package com.caiotcruz.mygamelist.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tb_conversations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_a_id", "user_b_id"})
})
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_a_id", nullable = false)
    private User userA;

    @ManyToOne
    @JoinColumn(name = "user_b_id", nullable = false)
    private User userB;

    @Column(columnDefinition = "TEXT")
    private String lastMessagePreview;

    @ManyToOne
    @JoinColumn(name = "last_message_sender_id")
    private User lastMessageSender;

    private LocalDateTime lastMessageAt;
}