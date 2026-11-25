package com.example.productos.controller;

import com.example.productos.config.SecurityConfig;
import com.example.productos.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductoController.class)
@Import(SecurityConfig.class)
class ProductoSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService service;

    @Test
    void getProductos_withAdmin_shouldReturn200() throws Exception {
        when(service.listarTodos()).thenReturn(List.of());

        mockMvc.perform(get("/productos")
                        .header("X-User-Username", "admin")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getProductos_withVendedor_shouldReturn200() throws Exception {
        when(service.listarTodos()).thenReturn(List.of());

        mockMvc.perform(get("/productos")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR"))
                .andExpect(status().isOk());
    }

    @Test
    void getProductos_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/productos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postProducto_withVendedor_shouldReturn403() throws Exception {
        mockMvc.perform(post("/productos")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
