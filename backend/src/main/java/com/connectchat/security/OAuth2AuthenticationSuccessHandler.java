package com.connectchat.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String accessToken = jwtTokenProvider.generateToken(userPrincipal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userPrincipal.getId());

        String redirectUri = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/callback")
                .queryParam("token", accessToken)
                .queryParam("refresh", refreshToken)
                .build()
                .toUriString();

        log.info("OAuth2 authentication successful for user: {}", userPrincipal.getEmail());
        response.sendRedirect(redirectUri);
    }
}
