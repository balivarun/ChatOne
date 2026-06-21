package com.connectchat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cloudinary cloudinary = new Cloudinary();
    private Cors cors = new Cors();
    private String frontendUrl = "http://localhost:3000";

    @Data
    public static class Jwt {
        private String secret = "changeme-at-least-256-bits-long-for-hs256-algorithm";
        private long expirationMs = 86400000L;
        private long refreshExpirationMs = 604800000L;
    }

    @Data
    public static class Cloudinary {
        private String cloudName;
        private String apiKey;
        private String apiSecret;
    }

    @Data
    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";
    }
}
