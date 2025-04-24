package com.example.springfile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // Import for disabling CSRF
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF protection - common for stateless APIs/SPAs
            .csrf(AbstractHttpConfigurer::disable)
            // Permit all requests for now.
            // TODO: Add more specific authorization rules if needed later
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
            // Note: If using stateful sessions, further configuration might be needed.
            // If using JWT or other token auth, filters would be added here.

        return http.build();
    }
}
