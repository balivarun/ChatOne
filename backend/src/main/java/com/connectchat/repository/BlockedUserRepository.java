package com.connectchat.repository;

import com.connectchat.entity.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    Optional<BlockedUser> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    List<BlockedUser> findByBlockerId(UUID blockerId);
}
