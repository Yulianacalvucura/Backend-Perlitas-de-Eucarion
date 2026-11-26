package com.example.reportes.service;

import com.example.reportes.exception.ServicioNoDisponibleException;
import com.example.reportes.model.Reporte;
import com.example.reportes.repository.ReporteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<WebClient, Chain> chains = new IdentityHashMap<>();

    @Mock
    private ReporteRepository reporteRepository;

    @Mock
    private WebClient productosWebClient;

    @Mock
    private WebClient materialesWebClient;

    @Mock
    private WebClient ventasWebClient;

    @Mock
    private WebClient pedidosWebClient;

    @Mock
    private WebClient gastosWebClient;

    private ReporteService service;

    @BeforeEach
    void setUp() {
        service = new ReporteService(
                reporteRepository,
                productosWebClient,
                materialesWebClient,
                ventasWebClient,
                pedidosWebClient,
                gastosWebClient);
    }

    @Test
    void generarReporteVentas_debeFiltrarPorRangoYDesglosarPorMetodoPago() throws Exception {
        LocalDate desde = LocalDate.of(2026, 6, 1);
        LocalDate hasta = LocalDate.of(2026, 6, 30);
        stubLista(ventasWebClient, "/ventas", Mono.just(List.of(
                venta(1L, "2026-06-10", "efectivo", 100.0),
                venta(2L, "2026-05-31", "efectivo", 500.0),
                venta(3L, "2026-06-20", "tarjeta", 50.0),
                venta(4L, "2026-07-01", "tarjeta", 700.0))));
        stubLista(productosWebClient, "/productos", Mono.just(List.of(Map.of("id", 1), Map.of("id", 2))));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        Reporte actual = service.generarReporteVentas(desde, hasta);

        assertEquals("ventas_por_periodo", actual.getTipo());
        JsonNode json = objectMapper.readTree(actual.getResultadoJson());
        assertEquals(2, json.get("cantidadVentas").asInt());
        assertEquals(150.0, json.get("totalVentas").asDouble());
        assertEquals(1, json.get("desglosePorMetodoPago").get("efectivo").get("cantidadVentas").asInt());
        assertEquals(100.0, json.get("desglosePorMetodoPago").get("efectivo").get("total").asDouble());
        assertEquals(50.0, json.get("desglosePorMetodoPago").get("tarjeta").get("total").asDouble());
        assertEquals(2, json.get("productos").asInt());
        verify(reporteRepository).save(any(Reporte.class));
    }

    @Test
    void generarReporteGastosPorCategoria_debeAgruparMontos() throws Exception {
        stubLista(gastosWebClient, "/gastos", Mono.just(List.of(
                gasto(1L, "2026-06-05", 30.0),
                gasto(1L, "2026-06-06", 20.0),
                gasto(2L, "2026-06-07", 50.0))));
        stubLista(gastosWebClient, "/categorias-gasto", Mono.just(List.of(
                Map.of("id", 1, "nombre", "Servicios"),
                Map.of("id", 2, "nombre", "Insumos"))));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        Reporte actual = service.generarReporteGastosPorCategoria();

        assertEquals("gastos_por_categoria", actual.getTipo());
        JsonNode json = objectMapper.readTree(actual.getResultadoJson());
        assertEquals(100.0, json.get("totalGastos").asDouble());
        JsonNode primera = json.get("gastosPorCategoria").get(0);
        assertEquals(1, primera.get("categoriaId").asInt());
        assertEquals("Servicios", primera.get("categoria").asText());
        assertEquals(2, primera.get("cantidadGastos").asInt());
        assertEquals(50.0, primera.get("total").asDouble());
    }

    @Test
    void generarReporteUtilidad_debeCalcularIngresosMenosGastosDelPeriodo() throws Exception {
        LocalDate desde = LocalDate.of(2026, 6, 1);
        LocalDate hasta = LocalDate.of(2026, 6, 30);
        stubLista(ventasWebClient, "/ventas", Mono.just(List.of(
                venta(1L, "2026-06-10", "efectivo", 200.0),
                venta(2L, "2026-05-10", "efectivo", 900.0))));
        stubLista(gastosWebClient, "/gastos", Mono.just(List.of(
                gasto(1L, "2026-06-12", 40.0),
                gasto(1L, "2026-07-12", 500.0))));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(inv -> inv.getArgument(0));

        Reporte actual = service.generarReporteUtilidad(desde, hasta);

        assertEquals("utilidad", actual.getTipo());
        JsonNode json = objectMapper.readTree(actual.getResultadoJson());
        assertEquals(200.0, json.get("ingresos").asDouble());
        assertEquals(40.0, json.get("gastos").asDouble());
        assertEquals(160.0, json.get("utilidad").asDouble());
        assertEquals("2026-06-01", json.get("desde").asText());
    }

    @Test
    void generarReporteVentas_whenServicioNoDisponible_debePropagar() {
        stubLista(ventasWebClient, "/ventas", Mono.error(
                WebClientResponseException.create(500, "Error", null, null, null)));

        assertThrows(ServicioNoDisponibleException.class,
                () -> service.generarReporteVentas(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)));
        verify(reporteRepository, never()).save(any(Reporte.class));
    }

    private Map<String, Object> venta(Long id, String fecha, String metodoPago, Double total) {
        return Map.of("id", id, "fecha", fecha, "metodoPago", metodoPago, "total", total);
    }

    private Map<String, Object> gasto(Long categoriaId, String fecha, Double monto) {
        return Map.of("categoriaId", categoriaId, "fecha", fecha, "monto", monto);
    }

    private void stubLista(WebClient client, String uri, Mono<?> respuesta) {
        Chain chain = chains.computeIfAbsent(client, c -> {
            Chain nueva = new Chain();
            nueva.uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
            when(c.get()).thenReturn(nueva.uriSpec);
            return nueva;
        });

        WebClient.ResponseSpec responseSpec = chain.responseSpecs
                .computeIfAbsent(uri, u -> mock(WebClient.ResponseSpec.class));
        WebClient.RequestHeadersSpec requestSpec = mock(WebClient.RequestHeadersSpec.class);
        when(chain.uriSpec.uri(uri)).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        doReturn(respuesta).when(responseSpec).bodyToMono(List.class);
    }

    private static class Chain {
        private WebClient.RequestHeadersUriSpec uriSpec;
        private final Map<String, WebClient.ResponseSpec> responseSpecs = new HashMap<>();
    }
}
