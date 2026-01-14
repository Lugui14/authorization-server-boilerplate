package com.luizguizl.authserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private OAuth2 oauth2 = new OAuth2();
    private List<RegisteredClientConfig> clients = new ArrayList<>();

    @Data
    public static class Jwt {
        private String issuer;
        private long accessTokenExpirationSeconds = 3600;
        private long refreshTokenExpirationSeconds = 604800; // 7 days
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins;
    }

    @Data
    public static class OAuth2 {
        private List<String> authorizedRedirectUris;
    }

    @Data
    public static class RegisteredClientConfig {
        private String clientId;
        private String clientSecret;
        private String clientName;
        private List<String> clientAuthenticationMethods = new ArrayList<>();
        private List<String> authorizationGrantTypes = new ArrayList<>();
        private List<String> redirectUris = new ArrayList<>();
        private List<String> scopes = new ArrayList<>();
        private Long accessTokenTimeToLiveSeconds;
        private Long refreshTokenTimeToLiveSeconds;
        private Boolean requireAuthorizationConsent;
        private Boolean requireProofKey;
    }
}

