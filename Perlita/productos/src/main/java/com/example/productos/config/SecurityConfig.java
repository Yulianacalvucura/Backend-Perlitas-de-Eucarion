package com.example.productos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/productos/**").hasAnyAuthority("ADMIN", "VENDEDOR")
                .requestMatchers(HttpMethod.PUT, "/productos/**").hasAnyAuthority("ADMIN", "VENDEDOR")
                .requestMatchers(HttpMethod.POST, "/productos/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/productos/**").hasAuthority("ADMIN")
                .requestMatchers(
                    "/api/v1/productos/v3/api-docs/",
                    "/api/v1/productos/v3/api-docs/**",
                    "/swagger-ui/",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception
                .accessDeniedHandler(new CustomAccessDeniedHandler())
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(401);
                    response.getWriter().write(String.format(
                        "{\"timestamp\": \"%s\", \"status\": 401, \"error\": \"Unauthorized\", \"mensaje\": \"Autenticación requerida: token faltante o inválido.\"}",
                        LocalDateTime.now()
                    ));
                })
            )
            .addFilterBefore(new RequestHeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}