package com.example.materiales.controller;

import com.example.materiales.config.SecurityConfig;
import com.example.materiales.model.Material;
import com.example.materiales.service.MaterialService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaterialController.class)
@Import(SecurityConfig.class)
class MaterialSecurityTest {

    private static final String MATERIAL_JSON = """
            {"nombre":"Perlas rosadas","stock":10,"stockMinimo":2}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaterialService service;

    @Test
    void listar_conAdministrador_debeResponder200() throws Exception {
        // Arrange
        when(service.listarTodos()).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get("/materiales")
                        .header("X-User-Username", "admin")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void listar_conVendedor_debeResponder200() throws Exception {
        // Arrange
        when(service.listarTodos()).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get("/materiales")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR"))
                .andExpect(status().isOk());
    }

    @Test
    void crear_conVendedor_debeResponder403YNoGuardar() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(post("/materiales")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MATERIAL_JSON))
                .andExpect(status().isForbidden());

        verify(service, never()).guardar(any(Material.class));
    }

    @Test
    void listar_sinAutenticacion_debeResponder401() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(get("/materiales"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }
}
