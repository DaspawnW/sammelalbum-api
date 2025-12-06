package com.daspawnw.sammelalbum.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private List<String> validationCodes;
    private JwtProperties jwt;
    private MailProperties mail;
    private String baseUrl;

    @Data
    public static class JwtProperties {
        private String secret;
        private Long expiration;
        private String passwordResetSecret;
        private Long passwordResetExpiration;
    }

    @Data
    public static class MailProperties {
        private String sender;
    }
}
