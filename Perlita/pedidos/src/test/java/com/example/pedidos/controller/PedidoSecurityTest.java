package com.example.pedidos.controller;

import com.example.pedidos.config.SecurityConfig;
import com.example.pedidos.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PedidoController.class)
@Import(SecurityConfig.class)
class PedidoSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoService service;

    @Test
    void listar_conAdministrador_debeResponder200() throws Exception {
        // Arrange
        when(service.listarTodos()).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get("/pedidos")
                        .header("X-User-Username", "admin")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void listar_conVendedor_debeResponder200() throws Exception {
        // Arrange
        when(service.listarTodos()).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get("/pedidos")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR"))
                .andExpect(status().isOk());
    }

    @Test
    void cambiarEstado_conVendedor_debeResponder403YNoCambiar() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(patch("/pedidos/1/estado")
                        .param("nuevoEstado", "en_proceso")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR"))
                .andExpect(status().isForbidden());

        verify(service, never()).cambiarEstado(anyLong(), anyString());
    }

    @Test
    void listar_sinAutenticacion_debeResponder401() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(get("/pedidos"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }
}
