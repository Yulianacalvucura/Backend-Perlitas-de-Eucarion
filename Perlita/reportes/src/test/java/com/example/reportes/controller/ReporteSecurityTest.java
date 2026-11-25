package com.example.reportes.controller;

import com.example.reportes.config.SecurityConfig;
import com.example.reportes.model.Reporte;
import com.example.reportes.service.ReporteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteController.class)
@Import(SecurityConfig.class)
class ReporteSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteService reporteService;

    @Test
    void listarReportes_conAdministrador_debeResponder200() throws Exception {
        // Arrange
        when(reporteService.listarTodos()).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get("/reportes")
                        .header("X-User-Username", "admin")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void listarReportes_conVendedor_debeResponder403() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(get("/reportes")
                        .header("X-User-Username", "vendedor")
                        .header("X-User-Roles", "VENDEDOR"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reporteService);
    }

    @Test
    void listarReportes_sinAutenticacion_debeResponder401() throws Exception {
        // Arrange
        // Act + Assert
        mockMvc.perform(get("/reportes"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reporteService);
    }

    @Test
    void generarReporteVentas_conAdministrador_debeResponder200() throws Exception {
        // Arrange
        when(reporteService.generarReporteVentas(any(), any())).thenReturn(new Reporte());

        // Act + Assert
        mockMvc.perform(get("/reportes/generar/ventas")
                        .param("desde", "2026-06-01")
                        .param("hasta", "2026-06-30")
                        .header("X-User-Username", "admin")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk());
    }
}
