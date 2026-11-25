package com.example.pedidos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_JWT = "bearer-jwt";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Perlitas – Servicio de Pedidos")
                        .version("1.0")
                        .description("Documentación de pedidos"))
                .servers(List.of(
                        new Server().url("http://localhost:9090").description("Gateway")
                ))
                .components(new Components().addSecuritySchemes(BEARER_JWT,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenido en POST /auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
    }
}
