package com.connectchat.service;

import com.connectchat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineStatusService {

    private static final String ONLINE_KEY_PREFIX = "online:";
    private static final long ONLINE_TTL_MINUTES = 5L;

    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;

    public void setUserOnline(UUID userId) {
        try {
            stringRedisTemplate.opsForValue().set(
                    ONLINE_KEY_PREFIX + userId, "1", ONLINE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis unavailable, skipping online status set for {}", userId);
        }
        userRepository.findById(userId).ifPresent(user -> {
            user.setIsOnline(true);
            userRepository.save(user);
        });
    }

    @Transactional
    public void setUserOffline(UUID userId) {
        try {
            stringRedisTemplate.delete(ONLINE_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Redis unavailable, skipping online status delete for {}", userId);
        }
        userRepository.findById(userId).ifPresent(user -> {
            user.setIsOnline(false);
            user.setLastSeen(Instant.now());
            userRepository.save(user);
        });
    }

    public boolean isUserOnline(UUID userId) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(ONLINE_KEY_PREFIX + userId));
        } catch (Exception e) {
            log.warn("Redis unavailable, defaulting isOnline=false for {}", userId);
            return false;
        }
    }

    public List<UUID> getOnlineUsers(List<UUID> userIds) {
        return userIds.stream()
                .filter(this::isUserOnline)
                .collect(Collectors.toList());
    }

    public void refreshOnlineStatus(UUID userId) {
        try {
            String key = ONLINE_KEY_PREFIX + userId;
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
                stringRedisTemplate.expire(key, ONLINE_TTL_MINUTES, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, skipping online status refresh for {}", userId);
        }
    }
}
