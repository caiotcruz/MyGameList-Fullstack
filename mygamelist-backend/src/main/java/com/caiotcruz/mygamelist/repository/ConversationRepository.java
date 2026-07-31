package com.caiotcruz.mygamelist.repository;

import com.caiotcruz.mygamelist.model.Conversation;
import com.caiotcruz.mygamelist.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("""
        SELECT c FROM Conversation c
        WHERE (c.userA = :u1 AND c.userB = :u2)
           OR (c.userA = :u2 AND c.userB = :u1)
    """)
    Optional<Conversation> findBetween(@Param("u1") User u1, @Param("u2") User u2);

    @Query("""
        SELECT c FROM Conversation c
        WHERE c.userA = :user OR c.userB = :user
        ORDER BY c.lastMessageAt DESC
    """)
    List<Conversation> findAllForUser(@Param("user") User user);
}