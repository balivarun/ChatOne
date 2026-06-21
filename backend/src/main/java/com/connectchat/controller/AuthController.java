package com.connectchat.controller;

import com.connectchat.dto.response.ApiResponse;
import com.connectchat.dto.response.AuthResponse;
import com.connectchat.dto.response.UserResponse;
import com.connectchat.security.UserPrincipal;
import com.connectchat.service.AuthService;
import com.connectchat.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @GetMapping("/google")
    public void initiateGoogleOAuth(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    /**
     * Mobile sign-in: native Android/iOS clients send the Google ID token obtained
     * via the Google Sign-In SDK. Backend verifies it and issues a JWT.
     */
    @PostMapping("/google/mobile")
    public ResponseEntity<ApiResponse<AuthResponse>> mobileGoogleSignIn(
            @RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("idToken is required"));
        }
        AuthResponse authResponse = authService.signInWithGoogleIdToken(idToken);
        return ResponseEntity.ok(ApiResponse.success("Signed in successfully", authResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("refreshToken is required"));
        }
        AuthResponse authResponse = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal != null) {
            authService.logout(principal.getId());
        }
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse userResponse = userService.getCurrentUser(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }
}
