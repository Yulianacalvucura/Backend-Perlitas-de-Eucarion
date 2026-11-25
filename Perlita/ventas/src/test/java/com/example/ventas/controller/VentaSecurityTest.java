package com.example.ventas.controller;

import com.example.ventas.config.SecurityConfig;
import com.example.ventas.service.VentaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VentaController.class)
@Import(SecurityConfig.class)
class VentaSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VentaService service;

    @Test
    void getVentas_withAdmin_shouldReturn200() throws Exception {
        when(service.listarTodas()).thenReturn(List.of());

        mockMvc.perform(get("/ventas")
                        .header("X-User-Username", "admin")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getVentas_withVendedor_shouldReturn200() throws Exception {
        when(service.listarTodas()).thenReturn(List.of());

        mockMvc.perform(get("/ventas")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR"))
                .andExpect(status().isOk());
    }

    @Test
    void getVentas_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/ventas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteVenta_withVendedor_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/ventas/1")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR"))
                .andExpect(status().isForbidden());
    }
}
