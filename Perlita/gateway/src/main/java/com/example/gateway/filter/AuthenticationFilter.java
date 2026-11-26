package com.example.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(1)
public class AuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secreto;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = rutaInterna(request.getRequestURI());

        if (path.startsWith("/swagger-ui") || path.contains("/v3/api-docs")) {
            return true;
        }

        return path.equals("/auth/login") && "POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            onError(response, "Token de autenticación faltante o con formato inválido.", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = authHeader.substring(7);

        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            onError(response, "El token JWT ha expirado o su firma es inválida.", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String username = claims.getSubject();
        Object rolesClaim = claims.get("roles");
        if (username == null || !(rolesClaim instanceof List<?> roles) || roles.isEmpty()
                || roles.stream().anyMatch(rol -> !(rol instanceof String))) {
            onError(response, "Token sin usuario o roles válidos.", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String rolesStr = String.join(",", roles.stream().map(String.class::cast).toList());
        boolean admin = roles.contains("ADMIN");
        boolean vendedor = roles.contains("VENDEDOR");
        String path = rutaInterna(request.getRequestURI());
        boolean consultaPermitida = "GET".equalsIgnoreCase(request.getMethod())
                && (path.equals("/auth/me")
                    || coincideRuta(path, "/productos")
                    || coincideRuta(path, "/categorias")
                    || coincideRuta(path, "/ventas")
                    || coincideRuta(path, "/pedidos"));

        if (!admin && (!vendedor || !consultaPermitida)) {
            onError(response, "Acceso denegado para este rol.", HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        HttpServletRequestWrapperWithHeaders mutada = new HttpServletRequestWrapperWithHeaders(request);
        mutada.addHeader("X-User-Username", username);
        mutada.addHeader("X-User-Roles", rolesStr);

        // Los errores de los microservicios no son errores de autenticación.
        filterChain.doFilter(mutada, response);
    }

    private String rutaInterna(String path) {
        return path.startsWith("/api/v1/") ? path.substring("/api/v1".length()) : path;
    }

    private boolean coincideRuta(String path, String base) {
        return path.equals(base) || path.startsWith(base + "/");
    }

    private void onError(HttpServletResponse response, String err, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");

        String httpError = (status == 401) ? "Unauthorized" : "Forbidden";
        String jsonResponse = String.format(
            "{\"timestamp\": \"%s\", \"status\": %d, \"error\": \"%s\", \"mensaje\": \"%s\"}",
            LocalDateTime.now(),
            status,
            httpError,
            err
        );

        response.getWriter().write(jsonResponse);
    }
}