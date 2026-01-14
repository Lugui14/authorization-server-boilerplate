package com.luizguizl.authserver.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthorizationServerConfig {

    private final AppProperties appProperties;

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());

        http.exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                ).oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/oauth2/**", "/login/**")
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/**", "/oauth2/**", "/login/**", "/error", "/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                )
                .oauth2Login(Customizer.withDefaults());

        return http.build();
    }

    /**
     * It will lead with a persistent storage of registered clients.
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);

        // Load clients from configuration
        if (appProperties.getClients() != null && !appProperties.getClients().isEmpty()) {
            for (AppProperties.RegisteredClientConfig clientConfig : appProperties.getClients()) {
                if (repository.findByClientId(clientConfig.getClientId()) == null) {
                    RegisteredClient client = buildClientFromConfig(clientConfig);
                    repository.save(client);
                }
            }
        }

        return repository;
    }

    private RegisteredClient buildClientFromConfig(AppProperties.RegisteredClientConfig config) {
        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(config.getClientId())
                .clientSecret(config.getClientSecret() != null ? passwordEncoder().encode(config.getClientSecret()) : null);

        if (config.getClientName() != null) {
            builder.clientName(config.getClientName());
        }

        if (config.getClientAuthenticationMethods() != null && !config.getClientAuthenticationMethods().isEmpty()) {
            config.getClientAuthenticationMethods().forEach(method -> {
                switch (method.toUpperCase()) {
                    case "CLIENT_SECRET_BASIC" -> builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                    case "CLIENT_SECRET_POST" -> builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                    case "CLIENT_SECRET_JWT" -> builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_JWT);
                    case "PRIVATE_KEY_JWT" -> builder.clientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT);
                    case "NONE" -> builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
                }
            });
        }

        if (config.getAuthorizationGrantTypes() != null && !config.getAuthorizationGrantTypes().isEmpty()) {
            config.getAuthorizationGrantTypes().forEach(grantType -> {
                switch (grantType.toUpperCase()) {
                    case "AUTHORIZATION_CODE" -> builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
                    case "REFRESH_TOKEN" -> builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
                    case "CLIENT_CREDENTIALS" -> builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
                    case "PASSWORD" -> builder.authorizationGrantType(new AuthorizationGrantType("password"));
                }
            });
        }

        if (config.getRedirectUris() != null && !config.getRedirectUris().isEmpty()) {
            config.getRedirectUris().forEach(builder::redirectUri);
        }

        if (config.getScopes() != null && !config.getScopes().isEmpty()) {
            config.getScopes().forEach(builder::scope);
        }

        TokenSettings.Builder tokenSettingsBuilder = TokenSettings.builder();
        if (config.getAccessTokenTimeToLiveSeconds() != null) {
            tokenSettingsBuilder.accessTokenTimeToLive(Duration.ofSeconds(config.getAccessTokenTimeToLiveSeconds()));
        } else {
            tokenSettingsBuilder.accessTokenTimeToLive(Duration.ofSeconds(appProperties.getJwt().getAccessTokenExpirationSeconds()));
        }
        if (config.getRefreshTokenTimeToLiveSeconds() != null) {
            tokenSettingsBuilder.refreshTokenTimeToLive(Duration.ofSeconds(config.getRefreshTokenTimeToLiveSeconds()));
        } else {
            tokenSettingsBuilder.refreshTokenTimeToLive(Duration.ofSeconds(appProperties.getJwt().getRefreshTokenExpirationSeconds()));
        }
        builder.tokenSettings(tokenSettingsBuilder.build());

        ClientSettings.Builder clientSettingsBuilder = ClientSettings.builder();
        if (config.getRequireAuthorizationConsent() != null) {
            clientSettingsBuilder.requireAuthorizationConsent(config.getRequireAuthorizationConsent());
        }
        if (config.getRequireProofKey() != null) {
            clientSettingsBuilder.requireProofKey(config.getRequireProofKey());
        }
        builder.clientSettings(clientSettingsBuilder.build());

        return builder.build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(appProperties.getJwt().getIssuer())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(appProperties.getCors().getAllowedOrigins());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

