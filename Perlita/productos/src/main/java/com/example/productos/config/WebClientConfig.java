package com.example.productos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${microservicio.materiales.url:http://localhost:8081}")
    private String materialesUrl;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter(WebClientAuthPropagationFilter.propagateAuthHeaders());
    }

    @Bean("materialesWebClient")
    public WebClient materialesWebClient() {
        return WebClient.builder()
                .baseUrl(materialesUrl)
                .filter(WebClientAuthPropagationFilter.propagateAuthHeaders())
                .build();
    }
}
