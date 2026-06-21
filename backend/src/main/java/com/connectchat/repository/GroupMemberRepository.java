package com.connectchat.repository;

import com.connectchat.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    List<GroupMember> findByGroupId(UUID groupId);
}
