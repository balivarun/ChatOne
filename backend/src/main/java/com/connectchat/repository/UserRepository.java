package com.connectchat.repository;

import com.connectchat.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    Page<User> findByDisplayNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String displayName, String email, Pageable pageable);
}
