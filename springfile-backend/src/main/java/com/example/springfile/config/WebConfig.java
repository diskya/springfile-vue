package com.example.springfile.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient; // Added import
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
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

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Apply CORS to all paths under /api
                .allowedOrigins("http://localhost:5173") // Allow requests from the default Vite dev server origin
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allowed HTTP methods
                .allowedHeaders("*") // Allow all headers
                .allowCredentials(true); // Allow credentials like cookies (if needed)
    }
}
