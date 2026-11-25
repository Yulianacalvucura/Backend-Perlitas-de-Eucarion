package com.example.categories.controller;

import com.example.categories.config.SecurityConfig;
import com.example.categories.model.Categoria;
import com.example.categories.service.CategoriaService;
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

@WebMvcTest(CategoriaController.class)
@Import(SecurityConfig.class)
class CategoriaSecurityTest {

    private static final String CATEGORIA_JSON = """
            {"nombre":"Velas","descripcion":"Velas artesanales","activo":1}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoriaService service;

    @Test
    void listar_conAdministrador_debeResponder200() throws Exception {
        // Arrange
        when(service.listarTodos()).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get("/categorias")
                        .header("X-User-Username", "admin")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void listar_conVendedor_debeResponder200() throws Exception {
        // Arrange
        when(service.listarTodos()).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get("/categorias")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR"))
                .andExpect(status().isOk());
    }

    @Test
    void crear_conVendedor_debeResponder403YNoGuardar() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(post("/categorias")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CATEGORIA_JSON))
                .andExpect(status().isForbidden());

        verify(service, never()).guardar(any(Categoria.class));
    }

    @Test
    void listar_sinAutenticacion_debeResponder401() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(get("/categorias"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }
}
