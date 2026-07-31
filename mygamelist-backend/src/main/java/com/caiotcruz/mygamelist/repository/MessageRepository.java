package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.model.Conversation;
import com.caiotcruz.mygamelist.model.Message;
import com.caiotcruz.mygamelist.model.User;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationOrderBySentAtAsc(Conversation conversation);

    @Modifying
    @Transactional
    @Query("""
        UPDATE Message m SET m.read = true
        WHERE m.conversation = :conversation
          AND m.sender <> :currentUser
          AND m.read = false
    """)
    void markAllAsRead(@Param("conversation") Conversation conversation, @Param("currentUser") User currentUser);

    @Query("""
        SELECT m.conversation.id, COUNT(m)
        FROM Message m
        WHERE m.conversation IN :conversations
          AND m.sender <> :currentUser
          AND m.read = false
        GROUP BY m.conversation.id
    """)
    List<Object[]> countUnreadByConversations(
            @Param("conversations") List<Conversation> conversations,
            @Param("currentUser") User currentUser
    );
}