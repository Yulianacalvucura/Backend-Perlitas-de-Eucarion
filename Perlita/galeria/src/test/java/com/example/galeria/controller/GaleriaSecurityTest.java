package com.example.galeria.controller;

import com.example.galeria.config.SecurityConfig;
import com.example.galeria.model.Galeria;
import com.example.galeria.service.GaleriaService;
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

@WebMvcTest(GaleriaController.class)
@Import(SecurityConfig.class)
class GaleriaSecurityTest {

    private static final String GALERIA_JSON = """
            {"productoId":1,"url":"https://ejemplo.local/perla.jpg","descripcion":"Perla rosa"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GaleriaService galeriaService;

    @Test
    void listar_conAdministrador_debeResponder200() throws Exception {
        // Arrange
        when(galeriaService.listarTodos()).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get("/galeria")
                        .header("X-User-Username", "admin")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void listar_conVendedor_debeResponder200() throws Exception {
        // Arrange
        when(galeriaService.listarTodos()).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get("/galeria")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR"))
                .andExpect(status().isOk());
    }

    @Test
    void guardar_conVendedor_debeResponder403YNoGuardar() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(post("/galeria")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GALERIA_JSON))
                .andExpect(status().isForbidden());

        verify(galeriaService, never()).guardar(any(Galeria.class));
    }

    @Test
    void listar_sinAutenticacion_debeResponder401() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(get("/galeria"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(galeriaService);
    }
}
