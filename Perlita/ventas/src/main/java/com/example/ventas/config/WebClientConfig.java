package com.example.ventas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${microservicio.productos.url:http://localhost:8082}")
    private String productosUrl;

    @Bean("productosWebClient")
    public WebClient productosWebClient() {
        return WebClient.builder().baseUrl(productosUrl).build();
    }
}
