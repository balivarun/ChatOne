package com.connectchat.repository;

import com.connectchat.entity.MessageRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageReadRepository extends JpaRepository<MessageRead, Long> {

    boolean existsByMessageIdAndUserId(UUID messageId, UUID userId);

    @Modifying
    @Query(value = "INSERT INTO message_reads (message_id, user_id, read_at) " +
                   "VALUES (:messageId, :userId, NOW()) ON CONFLICT DO NOTHING",
           nativeQuery = true)
    void upsertRead(@Param("messageId") UUID messageId, @Param("userId") UUID userId);
}
