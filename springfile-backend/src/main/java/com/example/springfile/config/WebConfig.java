package com.example.springfile.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
// Removed ViewControllerRegistry, EnableWebMvc, ResourceHandlerRegistry imports
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
// Removed @EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    // Inject the base URL from application.properties for flexibility
    @Value("${fastapi.service.url:http://localhost:8001}") // Default to localhost:8001
    private String fastapiServiceUrl;

    @Bean
    public WebClient fastapiWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(fastapiServiceUrl)
                .build();
    }

    // Removed addCorsMappings override - not needed for same-origin deployment
    // and might conflict with Spring Security CORS handling (if enabled later).

    // Removed addResourceHandlers method - rely on Spring Boot defaults
  
      // Removed addViewControllers method - rely on Spring Boot defaults for SPA fallback
  }
