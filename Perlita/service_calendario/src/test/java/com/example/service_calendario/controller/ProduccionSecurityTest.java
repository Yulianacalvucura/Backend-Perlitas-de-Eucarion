package com.example.service_calendario.controller;

import com.example.service_calendario.config.SecurityConfig;
import com.example.service_calendario.model.Bloqueo;
import com.example.service_calendario.service.BloqueoService;
import com.example.service_calendario.service.ProduccionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ProduccionController.class, BloqueoController.class})
@Import(SecurityConfig.class)
class ProduccionSecurityTest {

    private static final String BLOQUEO_JSON = """
            {"fechaInicio":"2026-12-24","fechaFin":"2026-12-31","motivo":"Vacaciones"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProduccionService produccionService;

    @MockitoBean
    private BloqueoService bloqueoService;

    @Test
    void listarProducciones_conAdministrador_debeResponder200() throws Exception {
        // Arrange
        when(produccionService.listarTodas()).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get("/api/v1/produccion")
                        .header("X-User-Username", "admin")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void listarProducciones_conVendedor_debeResponder200() throws Exception {
        // Arrange
        when(produccionService.listarTodas()).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get("/api/v1/produccion")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR"))
                .andExpect(status().isOk());
    }

    @Test
    void crearBloqueo_conVendedor_debeResponder403YNoCrear() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(post("/api/v1/produccion/bloqueos")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BLOQUEO_JSON))
                .andExpect(status().isForbidden());

        verify(bloqueoService, never()).crear(any(Bloqueo.class));
    }

    @Test
    void crearBloqueo_conAdministrador_debeResponder201() throws Exception {
        // Arrange
        when(bloqueoService.crear(any(Bloqueo.class))).thenReturn(new Bloqueo());

        // Act + Assert
        mockMvc.perform(post("/api/v1/produccion/bloqueos")
                        .header("X-User-Username", "admin")
                        .header("X-User-Roles", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BLOQUEO_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void eliminarProduccion_conVendedor_debeResponder403YNoEliminar() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(delete("/api/v1/produccion/1")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR"))
                .andExpect(status().isForbidden());

        verify(produccionService, never()).eliminar(1);
    }

    @Test
    void listarProducciones_sinAutenticacion_debeResponder401() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(get("/api/v1/produccion"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(produccionService);
    }
}
