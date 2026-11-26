package com.example.proveedores.controller;

import com.example.proveedores.config.SecurityConfig;
import com.example.proveedores.model.Proveedor;
import com.example.proveedores.service.ProveedorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProveedorController.class)
@Import(SecurityConfig.class)
class ProveedorSecurityTest {

    private static final String PROVEEDOR_JSON = """
            {"nombre":"Proveedor prueba","activo":1}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProveedorService service;

    @Test
    void listar_conAdministrador_debeResponder200() throws Exception {
        // Arrange
        when(service.listarTodos()).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get("/proveedores")
                        .header("X-User-Username", "admin")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void listar_conVendedor_debeResponder403() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(get("/proveedores")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void listar_sinAutenticacion_debeResponder401() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(get("/proveedores"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void crear_conAdministrador_debeResponder201() throws Exception {
        // Arrange
        when(service.guardar(any(Proveedor.class))).thenReturn(new Proveedor());

        // Act + Assert
        mockMvc.perform(post("/proveedores")
                        .header("X-User-Username", "admin")
                        .header("X-User-Roles", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROVEEDOR_JSON))
                .andExpect(status().isCreated());
    }
}
