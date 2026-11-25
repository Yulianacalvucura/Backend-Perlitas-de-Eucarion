package com.example.service_auth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void usuarioDuplicado_shouldReturn409() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUsuarioDuplicado(new UsuarioDuplicadoException("El nombre de usuario ya existe."));

        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    void credencialesInvalidas_shouldReturn401() {
        ResponseEntity<ErrorResponse> response =
                handler.handleCredencialesInvalidas(new CredencialesInvalidasException("Credenciales inválidas"));

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void rolInvalido_shouldReturn400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleRolInvalido(new RolInvalidoException("Rol no permitido"));

        assertEquals(400, response.getStatusCode().value());
    }
}
