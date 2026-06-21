package com.connectchat.repository;

import com.connectchat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.user.id = :userId AND c.type = 'DIRECT' " +
           "ORDER BY (SELECT MAX(m.createdAt) FROM Message m WHERE m.conversation = c) DESC")
    List<Conversation> findDirectConversationsByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM Conversation c JOIN c.participants p1 JOIN c.participants p2 " +
           "WHERE p1.user.id = :user1 AND p2.user.id = :user2 AND c.type = 'DIRECT'")
    Optional<Conversation> findDirectConversationBetween(@Param("user1") UUID user1, @Param("user2") UUID user2);

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.user.id = :userId " +
           "ORDER BY (SELECT MAX(m.createdAt) FROM Message m WHERE m.conversation = c) DESC NULLS LAST")
    List<Conversation> findAllConversationsByUserId(@Param("userId") UUID userId);
}
