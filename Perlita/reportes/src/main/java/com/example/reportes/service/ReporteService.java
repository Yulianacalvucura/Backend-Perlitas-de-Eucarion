package com.example.reportes.service;

import com.example.reportes.exception.ServicioNoDisponibleException;
import com.example.reportes.model.Reporte;
import com.example.reportes.repository.ReporteRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final WebClient productosWebClient;
    private final WebClient materialesWebClient;
    private final WebClient ventasWebClient;
    private final WebClient pedidosWebClient;
    private final WebClient gastosWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReporteService(ReporteRepository reporteRepository,
                          @Qualifier("productosWebClient") WebClient productosWebClient,
                          @Qualifier("materialesWebClient") WebClient materialesWebClient,
                          @Qualifier("ventasWebClient") WebClient ventasWebClient,
                          @Qualifier("pedidosWebClient") WebClient pedidosWebClient,
                          @Qualifier("gastosWebClient") WebClient gastosWebClient) {
        this.reporteRepository = reporteRepository;
        this.productosWebClient = productosWebClient;
        this.materialesWebClient = materialesWebClient;
        this.ventasWebClient = ventasWebClient;
        this.pedidosWebClient = pedidosWebClient;
        this.gastosWebClient = gastosWebClient;
    }

    public List<Reporte> listarTodos() {
        return reporteRepository.findAll();
    }

    public Optional<Reporte> buscarPorId(Long id) {
        return reporteRepository.findById(id);
    }

    public Reporte guardar(Reporte reporte) {
        return reporteRepository.save(reporte);
    }

    public void eliminar(Long id) {
        reporteRepository.deleteById(id);
    }

    @SuppressWarnings("unchecked")
    public Reporte generarReporteVentas(LocalDate desde, LocalDate hasta) {
        LocalDate inicio = desde != null ? desde : LocalDate.now().minusDays(30);
        LocalDate fin = hasta != null ? hasta : LocalDate.now();

        List<Map<String, Object>> ventas = mapas(obtenerLista(ventasWebClient, "/ventas", "ventas"));
        List<Map<String, Object>> productos = mapas(obtenerLista(productosWebClient, "/productos", "productos"));

        List<Map<String, Object>> ventasPeriodo = ventas.stream()
                .filter(venta -> enRango(venta.get("fecha"), inicio, fin))
                .toList();

        double totalVentas = ventasPeriodo.stream()
                .mapToDouble(venta -> numero(venta.get("total")))
                .sum();

        Map<String, Map<String, Object>> desglose = new LinkedHashMap<>();
        for (Map<String, Object> venta : ventasPeriodo) {
            Object metodo = venta.get("metodoPago");
            String clave = metodo != null ? metodo.toString() : "desconocido";
            Map<String, Object> item = desglose.computeIfAbsent(clave, k -> {
                Map<String, Object> nuevo = new LinkedHashMap<>();
                nuevo.put("cantidadVentas", 0L);
                nuevo.put("total", 0.0);
                return nuevo;
            });
            item.put("cantidadVentas", ((Long) item.get("cantidadVentas")) + 1);
            item.put("total", ((Double) item.get("total")) + numero(venta.get("total")));
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("ventas", ventas.size());
        resultado.put("productos", productos.size());
        resultado.put("cantidadVentas", ventasPeriodo.size());
        resultado.put("totalVentas", totalVentas);
        resultado.put("desglosePorMetodoPago", desglose);

        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("desde", inicio.toString());
        parametros.put("hasta", fin.toString());

        return guardarReporte("ventas_por_periodo", parametros, resultado);
    }

    public Reporte generarReporteStockCritico() {
        List<Map<String, Object>> materiales = mapas(
                obtenerLista(materialesWebClient, "/materiales/stock-bajo", "materiales"));

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("materialesStockBajo", materiales.size());

        return guardarReporte("stock_critico", Map.of(), resultado);
    }

    public Reporte generarReportePedidos() {
        List<Map<String, Object>> pedidos = mapas(obtenerLista(pedidosWebClient, "/pedidos", "pedidos"));

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalPedidos", pedidos.size());

        return guardarReporte("estado_pedidos", Map.of(), resultado);
    }

    @SuppressWarnings("unchecked")
    public Reporte generarReporteGastosPorCategoria() {
        List<Map<String, Object>> gastos = mapas(obtenerLista(gastosWebClient, "/gastos", "gastos"));
        List<Map<String, Object>> categorias = mapas(
                obtenerLista(gastosWebClient, "/categorias-gasto", "categorías de gasto"));

        Map<String, String> nombres = new LinkedHashMap<>();
        for (Map<String, Object> categoria : categorias) {
            String id = claveCategoria(categoria.get("id"));
            if (id != null) {
                nombres.put(id, String.valueOf(categoria.get("nombre")));
            }
        }

        Map<String, Map<String, Object>> porCategoria = new LinkedHashMap<>();
        for (Map<String, Object> gasto : gastos) {
            Object categoriaId = gasto.get("categoriaId");
            Map<String, Object> item = porCategoria.computeIfAbsent(claveCategoria(categoriaId), id -> {
                Map<String, Object> nuevo = new LinkedHashMap<>();
                nuevo.put("categoriaId", categoriaId);
                nuevo.put("categoria", nombres.getOrDefault(id, "Sin categoría"));
                nuevo.put("cantidadGastos", 0L);
                nuevo.put("total", 0.0);
                return nuevo;
            });
            item.put("cantidadGastos", ((Long) item.get("cantidadGastos")) + 1);
            item.put("total", ((Double) item.get("total")) + numero(gasto.get("monto")));
        }

        double totalGastos = porCategoria.values().stream()
                .mapToDouble(item -> (Double) item.get("total"))
                .sum();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("gastosPorCategoria", new java.util.ArrayList<>(porCategoria.values()));
        resultado.put("totalGastos", totalGastos);

        return guardarReporte("gastos_por_categoria", Map.of(), resultado);
    }

    public Reporte generarReporteUtilidad(LocalDate desde, LocalDate hasta) {
        LocalDate inicio = desde != null ? desde : LocalDate.now().minusDays(30);
        LocalDate fin = hasta != null ? hasta : LocalDate.now();

        List<Map<String, Object>> ventas = mapas(obtenerLista(ventasWebClient, "/ventas", "ventas"));
        List<Map<String, Object>> gastos = mapas(obtenerLista(gastosWebClient, "/gastos", "gastos"));

        double ingresos = ventas.stream()
                .filter(venta -> enRango(venta.get("fecha"), inicio, fin))
                .mapToDouble(venta -> numero(venta.get("total")))
                .sum();
        double totalGastos = gastos.stream()
                .filter(gasto -> enRango(gasto.get("fecha"), inicio, fin))
                .mapToDouble(gasto -> numero(gasto.get("monto")))
                .sum();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("desde", inicio.toString());
        resultado.put("hasta", fin.toString());
        resultado.put("ingresos", ingresos);
        resultado.put("gastos", totalGastos);
        resultado.put("utilidad", ingresos - totalGastos);

        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("desde", inicio.toString());
        parametros.put("hasta", fin.toString());

        return guardarReporte("utilidad", parametros, resultado);
    }

    private Reporte guardarReporte(String tipo, Map<String, Object> parametros, Map<String, Object> resultado) {
        Reporte reporte = new Reporte();
        reporte.setTipo(tipo);
        reporte.setFechaGeneracion(LocalDate.now());
        reporte.setParametros(escribirJson(parametros));
        reporte.setResultadoJson(escribirJson(resultado));
        return reporteRepository.save(reporte);
    }

    private List<?> obtenerLista(WebClient client, String uri, String servicio) {
        try {
            List<?> lista = client.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(List.class)
                    .onErrorResume(WebClientResponseException.NotFound.class,
                            e -> Mono.just(Collections.emptyList()))
                    .onErrorMap(WebClientRequestException.class,
                            e -> new ServicioNoDisponibleException("Servicio de " + servicio + " no disponible", e))
                    .onErrorMap(WebClientResponseException.class,
                            e -> new ServicioNoDisponibleException("Servicio de " + servicio + " no disponible", e))
                    .block();
            return lista != null ? lista : Collections.emptyList();
        } catch (ServicioNoDisponibleException e) {
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapas(List<?> lista) {
        return lista.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    private String claveCategoria(Object valor) {
        if (valor == null) {
            return null;
        }
        return valor instanceof Number numero ? Long.toString(numero.longValue()) : valor.toString();
    }

    private boolean enRango(Object fecha, LocalDate desde, LocalDate hasta) {
        if (fecha == null) {
            return false;
        }
        LocalDate valor = fecha instanceof LocalDate ld ? ld : LocalDate.parse(fecha.toString());
        return !valor.isBefore(desde) && !valor.isAfter(hasta);
    }

    private double numero(Object valor) {
        return valor instanceof Number numero ? numero.doubleValue() : 0.0;
    }

    private String escribirJson(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("No se pudo serializar el resultado del reporte", e);
        }
    }
}
