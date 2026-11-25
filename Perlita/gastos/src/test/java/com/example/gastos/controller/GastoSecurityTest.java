package com.example.gastos.controller;

import com.example.gastos.config.SecurityConfig;
import com.example.gastos.service.GastoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GastoController.class)
@Import(SecurityConfig.class)
class GastoSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GastoService service;

    @Test
    void getGastos_withAdmin_shouldReturn200() throws Exception {
        when(service.listarTodos()).thenReturn(List.of());

        mockMvc.perform(get("/gastos")
                        .header("X-User-Username", "admin")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getGastos_withVendedor_shouldReturn403() throws Exception {
        mockMvc.perform(get("/gastos")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getGastos_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/gastos"))
                .andExpect(status().isUnauthorized());
    }
}
