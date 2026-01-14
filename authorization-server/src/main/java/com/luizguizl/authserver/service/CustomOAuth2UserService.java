package com.luizguizl.authserver.service;

import com.luizguizl.authserver.entity.AuthProvider;
import com.luizguizl.authserver.entity.User;
import com.luizguizl.authserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        String providerId = oAuth2User.getAttribute("sub");
        if (providerId == null) {
            providerId = oAuth2User.getAttribute("id") != null ?
                    Objects.requireNonNull(oAuth2User.getAttribute("id")).toString() :
                    oAuth2User.getName();
        }

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (name == null) {
            name = oAuth2User.getAttribute("login");
        }

        log.info("OAuth2 user login - Provider: {}, ProviderId: {}, Email: {}", provider, providerId, email);

        final String finalProviderId = providerId;
        final String finalEmail = email;
        final String finalName = name;

        User user = userRepository.findByProviderAndProviderId(provider, finalProviderId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(finalEmail)
                            .name(finalName)
                            .provider(provider)
                            .providerId(finalProviderId)
                            .password("") // OAuth users don't have password
                            .enabled(true)
                            .accountNonExpired(true)
                            .accountNonLocked(true)
                            .credentialsNonExpired(true)
                            .roles(Set.of("USER"))
                            .build();
                    return userRepository.save(newUser);
                });

        return oAuth2User;
    }
}

