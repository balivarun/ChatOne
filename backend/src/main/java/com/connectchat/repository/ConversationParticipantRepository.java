package com.connectchat.repository;

import com.connectchat.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    List<ConversationParticipant> findByUserId(UUID userId);

    List<ConversationParticipant> findByConversationId(UUID conversationId);
}
