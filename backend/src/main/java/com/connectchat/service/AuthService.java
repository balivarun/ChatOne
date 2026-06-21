package com.connectchat.service;

import com.connectchat.dto.response.AuthResponse;
import com.connectchat.dto.response.UserResponse;
import com.connectchat.entity.User;
import com.connectchat.exception.ResourceNotFoundException;
import com.connectchat.repository.UserRepository;
import com.connectchat.security.JwtTokenProvider;
import com.connectchat.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final OnlineStatusService onlineStatusService;
    private final RestTemplate restTemplate;

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        UserPrincipal principal = UserPrincipal.fromUser(user);
        String newAccessToken = jwtTokenProvider.generateToken(principal);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        UserResponse userResponse = mapToUserResponse(user);
        return AuthResponse.of(newAccessToken, newRefreshToken, userResponse);
    }

    /**
     * Mobile sign-in: verify Google ID token via Google's tokeninfo endpoint,
     * then find or create the user and issue a JWT.
     * Called by native Android/iOS clients which obtain an ID token via Google Sign-In SDK.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public AuthResponse signInWithGoogleIdToken(String idToken) {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new IllegalArgumentException("Invalid Google ID token");
        }

        Map<String, Object> payload = response.getBody();
        String googleId = (String) payload.get("sub");
        String email    = (String) payload.get("email");
        String name     = (String) payload.get("name");
        String picture  = (String) payload.get("picture");

        if (googleId == null || email == null) {
            throw new IllegalArgumentException("Incomplete Google token payload");
        }

        User user = userRepository.findByGoogleId(googleId).orElseGet(() -> {
            User newUser = new User();
            newUser.setGoogleId(googleId);
            newUser.setEmail(email);
            newUser.setDisplayName(name != null ? name : email.split("@")[0]);
            newUser.setAvatarUrl(picture);
            return userRepository.save(newUser);
        });

        UserPrincipal principal = UserPrincipal.fromUser(user);
        String accessToken  = jwtTokenProvider.generateToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        log.info("Mobile Google sign-in: user {} ({})", user.getEmail(), user.getId());
        return AuthResponse.of(accessToken, refreshToken, mapToUserResponse(user));
    }

    @Transactional
    public void logout(UUID userId) {
        onlineStatusService.setUserOffline(userId);
        log.info("User {} logged out", userId);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .isOnline(user.getIsOnline())
                .lastSeen(user.getLastSeen())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
