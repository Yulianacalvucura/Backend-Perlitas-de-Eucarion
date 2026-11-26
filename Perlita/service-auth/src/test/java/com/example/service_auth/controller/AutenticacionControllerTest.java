package com.example.service_auth.controller;

import com.example.service_auth.config.SecurityConfig;
import com.example.service_auth.dto.AuthRequest;
import com.example.service_auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AutenticacionController.class)
@Import(SecurityConfig.class)
class AutenticacionControllerTest {

    private static final String LOGIN_JSON = """
            {"nombreUsuario":"admin","contrasena":"admin123"}
            """;

    private static final String REGISTRO_JSON = """
            {"nombreUsuario":"vendedor1","contrasena":"vendedor123","correo":"vendedor@perlitas.cl","roles":["VENDEDOR"]}
            """;

    private static final String REGISTRO_PASSWORD_CORTA_JSON = """
            {"nombreUsuario":"vendedor1","contrasena":"corta","correo":"vendedor@perlitas.cl","roles":["VENDEDOR"]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    void login_sinAutenticacion_debeSerPublicoYResponder200() throws Exception {
        // Arrange
        when(authService.login("admin", "admin123")).thenReturn("token123");

        // Act + Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void registrar_conVendedor_debeResponder403YNoRegistrar() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(post("/auth/registrar")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isForbidden());

        verify(authService, never()).registrar(any(AuthRequest.class));
    }

    @Test
    void registrar_conAdministrador_debeResponder200() throws Exception {
        // Arrange
        when(authService.registrar(any(AuthRequest.class))).thenReturn("Usuario Registrado");

        // Act + Assert
        mockMvc.perform(post("/auth/registrar")
                        .header("X-User-Username", "admin")
                        .header("X-User-Roles", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void registrar_sinAutenticacion_debeResponder401YNoRegistrar() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isUnauthorized());

        verify(authService, never()).registrar(any(AuthRequest.class));
    }

    @Test
    void registrar_conContrasenaCorta_debeResponder400YNoRegistrar() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(post("/auth/registrar")
                        .header("X-User-Username", "admin")
                        .header("X-User-Roles", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_PASSWORD_CORTA_JSON))
                .andExpect(status().isBadRequest());

        verify(authService, never()).registrar(any(AuthRequest.class));
    }
}
