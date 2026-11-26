package com.example.ventas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${microservicio.productos.url:http://localhost:8082}")
    private String productosUrl;

    @Value("${microservicio.pedidos.url:http://localhost:8085}")
    private String pedidosUrl;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter(WebClientAuthPropagationFilter.propagateAuthHeaders());
    }

    @Bean("productosWebClient")
    public WebClient productosWebClient() {
        return WebClient.builder()
                .baseUrl(productosUrl)
                .filter(WebClientAuthPropagationFilter.propagateAuthHeaders())
                .build();
    }

    @Bean("pedidosWebClient")
    public WebClient pedidosWebClient() {
        return WebClient.builder()
                .baseUrl(pedidosUrl)
                .filter(WebClientAuthPropagationFilter.propagateAuthHeaders())
                .build();
    }
}
