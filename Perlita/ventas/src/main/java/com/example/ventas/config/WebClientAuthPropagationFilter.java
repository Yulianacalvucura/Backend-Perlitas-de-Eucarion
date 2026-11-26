package com.example.ventas.config;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

public class WebClientAuthPropagationFilter {

    private WebClientAuthPropagationFilter() {
    }

    public static ExchangeFilterFunction propagateAuthHeaders() {
        return (request, next) -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return next.exchange(request);
            }
            HttpServletRequest httpRequest = attrs.getRequest();
            String username = httpRequest.getHeader("X-User-Username");
            String roles = httpRequest.getHeader("X-User-Roles");

            ClientRequest.Builder builder = ClientRequest.from(request);
            if (username != null && !username.isBlank()) {
                builder.header("X-User-Username", username);
            }
            if (roles != null && !roles.isBlank()) {
                builder.header("X-User-Roles", roles);
            }
            return next.exchange(builder.build());
        };
    }
}
