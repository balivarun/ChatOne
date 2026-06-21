package com.connectchat.repository;

import com.connectchat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :convId " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) AND m.isDeleted = false")
    Page<Message> searchMessages(@Param("convId") UUID convId,
                                  @Param("query") String query,
                                  Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :convId " +
           "AND m.isDeleted = false " +
           "AND m.id NOT IN (SELECT mr.message.id FROM MessageRead mr WHERE mr.user.id = :userId)")
    long countUnreadMessages(@Param("convId") UUID convId, @Param("userId") UUID userId);

    Optional<Message> findTopByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID conversationId);
}
