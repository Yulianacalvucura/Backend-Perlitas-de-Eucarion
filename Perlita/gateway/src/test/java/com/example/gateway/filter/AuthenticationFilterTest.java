package com.example.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationFilterTest {

    private static final String SECRETO = "493358340235739058340598340598340598340598340598";

    private AuthenticationFilter filtro;

    @BeforeEach
    void setUp() {
        filtro = new AuthenticationFilter();
        ReflectionTestUtils.setField(filtro, "secreto", SECRETO);
    }

    @Test
    void filtrar_sinToken_debeResponder401YNoEjecutarCadena() throws Exception {
        // Arrange
        MockHttpServletRequest solicitud = new MockHttpServletRequest("GET", "/pedidos");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        AtomicBoolean cadenaEjecutada = new AtomicBoolean(false);
        FilterChain cadena = (req, res) -> cadenaEjecutada.set(true);

        // Act
        filtro.doFilter(solicitud, respuesta, cadena);

        // Assert
        assertEquals(401, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("Token de autenticación faltante"));
        assertFalse(cadenaEjecutada.get());
    }

    @Test
    void filtrar_conTokenInvalido_debeResponder401YNoEjecutarCadena() throws Exception {
        // Arrange
        MockHttpServletRequest solicitud = new MockHttpServletRequest("GET", "/pedidos");
        solicitud.addHeader("Authorization", "Bearer token-invalido");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        AtomicBoolean cadenaEjecutada = new AtomicBoolean(false);
        FilterChain cadena = (req, res) -> cadenaEjecutada.set(true);

        // Act
        filtro.doFilter(solicitud, respuesta, cadena);

        // Assert
        assertEquals(401, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("expirado o su firma es inválida"));
        assertFalse(cadenaEjecutada.get());
    }

    @Test
    void filtrar_conTokenExpirado_debeResponder401YNoEjecutarCadena() throws Exception {
        // Arrange
        String token = crearToken("admin", List.of("ADMIN"), Date.from(Instant.now().minusSeconds(60)));
        MockHttpServletRequest solicitud = new MockHttpServletRequest("GET", "/pedidos");
        solicitud.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        AtomicBoolean cadenaEjecutada = new AtomicBoolean(false);
        FilterChain cadena = (req, res) -> cadenaEjecutada.set(true);

        // Act
        filtro.doFilter(solicitud, respuesta, cadena);

        // Assert
        assertEquals(401, respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("expirado o su firma es inválida"));
        assertFalse(cadenaEjecutada.get());
    }

    @Test
    void filtrar_conTokenValido_debeEjecutarCadenaIgnorarHeadersDelClienteYAnadirIdentidad() throws Exception {
        // Arrange
        String token = crearToken("admin", List.of("ADMIN", "VENDEDOR"),
                Date.from(Instant.now().plusSeconds(300)));
        MockHttpServletRequest solicitud = new MockHttpServletRequest("GET", "/pedidos");
        solicitud.addHeader("Authorization", "Bearer " + token);
        solicitud.addHeader("X-User-Username", "intruso");
        solicitud.addHeader("X-User-Roles", "CLIENTE");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        AtomicBoolean cadenaEjecutada = new AtomicBoolean(false);
        AtomicReference<HttpServletRequest> solicitacionFiltrada = new AtomicReference<>();
        FilterChain cadena = (req, res) -> {
            cadenaEjecutada.set(true);
            solicitacionFiltrada.set((HttpServletRequest) req);
        };

        // Act
        filtro.doFilter(solicitud, respuesta, cadena);

        // Assert
        assertTrue(cadenaEjecutada.get());
        HttpServletRequest filtrada = solicitacionFiltrada.get();
        assertNotSame(solicitud, filtrada);
        assertEquals("admin", filtrada.getHeader("X-User-Username"));
        assertEquals("ADMIN,VENDEDOR", filtrada.getHeader("X-User-Roles"));
        assertEquals(1, Collections.list(filtrada.getHeaders("X-User-Roles")).size());
        assertEquals("intruso", solicitud.getHeader("X-User-Username"));
    }

    private String crearToken(String usuario, List<String> roles, Date expiracion) {
        Key clave = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(usuario)
                .claim("roles", roles)
                .setExpiration(expiracion)
                .signWith(clave)
                .compact();
    }
}
