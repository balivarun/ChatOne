package com.connectchat.security;

import com.connectchat.entity.User;
import com.connectchat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return processOAuth2User(oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String googleId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        if (googleId == null || email == null) {
            throw new OAuth2AuthenticationException("Missing required OAuth2 attributes: sub and email");
        }

        User user = userRepository.findByGoogleId(googleId)
                .map(existingUser -> updateExistingUser(existingUser, name, picture))
                .orElseGet(() -> registerNewUser(googleId, email, name, picture));

        return UserPrincipal.fromUser(user, attributes);
    }

    private User updateExistingUser(User user, String name, String picture) {
        if (name != null && !name.isBlank()) {
            user.setDisplayName(name);
        }
        if (picture != null) {
            user.setAvatarUrl(picture);
        }
        return userRepository.save(user);
    }

    private User registerNewUser(String googleId, String email, String name, String picture) {
        User newUser = User.builder()
                .googleId(googleId)
                .email(email)
                .displayName(name != null && !name.isBlank() ? name : email.split("@")[0])
                .avatarUrl(picture)
                .isOnline(false)
                .build();
        return userRepository.save(newUser);
    }
}
