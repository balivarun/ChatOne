package com.connectchat.repository;

import com.connectchat.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    @Query("SELECT g FROM Group g JOIN g.members m WHERE m.user.id = :userId")
    List<Group> findGroupsByUserId(@Param("userId") UUID userId);

    Page<Group> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
