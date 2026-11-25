package com.example.ventas.service;

import com.example.ventas.exception.ResourceNotFoundException;
import com.example.ventas.model.Venta;
import com.example.ventas.model.VentaProducto;
import com.example.ventas.repository.VentaProductoRepository;
import com.example.ventas.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private VentaProductoRepository ventaProductoRepository;

    @Mock
    private WebClient productosWebClient;

    @Mock
    private WebClient pedidosWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private WebClient.RequestHeadersUriSpec pedidosRequestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec pedidosRequestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec pedidosResponseSpec;

    @InjectMocks
    private VentaService service;

    @Test
    void listarTodas_shouldReturnAllSales() {
        // Arrange
        List<Venta> expected = List.of(new Venta());
        when(ventaRepository.findAll()).thenReturn(expected);

        // Act
        List<Venta> actual = service.listarTodas();

        // Assert
        assertEquals(expected, actual);
        verify(ventaRepository).findAll();
    }

    @Test
    void buscarPorId_whenExists_shouldReturnSale() {
        // Arrange
        Venta v = new Venta();
        v.setId(1L);
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(v));

        // Act
        Optional<Venta> actual = service.buscarPorId(1L);

        // Assert
        assertTrue(actual.isPresent());
        assertEquals(1L, actual.get().getId());
        verify(ventaRepository).findById(1L);
    }

    @Test
    void buscarPorId_whenNotExists_shouldReturnEmpty() {
        // Arrange
        when(ventaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        Optional<Venta> actual = service.buscarPorId(99L);

        // Assert
        assertTrue(actual.isEmpty());
        verify(ventaRepository).findById(99L);
    }

    @Test
    void buscarPorCliente_shouldReturnSalesByClient() {
        // Arrange
        Venta v = new Venta();
        v.setClienteId(5L);
        List<Venta> expected = List.of(v);
        when(ventaRepository.findByClienteId(5L)).thenReturn(expected);

        // Act
        List<Venta> actual = service.buscarPorCliente(5L);

        // Assert
        assertEquals(expected, actual);
        verify(ventaRepository).findByClienteId(5L);
    }

    @Test
    void crearVenta_conProductos_debeTomarPrecioDesdeProductosYDescontarStock() {
        // Arrange
        Venta venta = new Venta();
        venta.setClienteId(1L);
        VentaProducto vp = new VentaProducto();
        vp.setProductoId(10L);
        vp.setCantidad(2);
        vp.setPrecioUnitario(999.0);
        venta.setProductos(List.of(vp));

        Map<String, Object> producto = producto(10L, true, 5, 100.0);
        stubObtenerProducto(10L, producto);
        stubDescontarStock(10L);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Venta actual = service.crearVenta(venta);

        // Assert
        assertEquals(200.0, actual.getTotal());
        assertEquals(100.0, actual.getProductos().get(0).getPrecioUnitario());
        assertEquals(LocalDate.now(), actual.getFecha());
        verify(ventaRepository).save(any(Venta.class));
        verify(ventaProductoRepository).saveAll(anyList());
        verify(requestBodySpec).bodyValue(Map.of("ajuste", -2));
    }

    @Test
    void crearVenta_whenProductoInactivo_debeFallarSinGuardarNiDescontar() {
        // Arrange
        Venta venta = new Venta();
        venta.setClienteId(1L);
        VentaProducto vp = new VentaProducto();
        vp.setProductoId(10L);
        vp.setCantidad(1);
        venta.setProductos(List.of(vp));
        stubObtenerProducto(10L, producto(10L, false, 5, 100.0));

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.crearVenta(venta));
        assertTrue(ex.getMessage().contains("no está activo"));
        verify(ventaRepository, never()).save(any(Venta.class));
        verify(productosWebClient, never()).patch();
    }

    @Test
    void crearVenta_whenStockInsuficiente_debeFallarSinGuardarNiDescontar() {
        // Arrange
        Venta venta = new Venta();
        venta.setClienteId(1L);
        VentaProducto vp = new VentaProducto();
        vp.setProductoId(10L);
        vp.setCantidad(5);
        venta.setProductos(List.of(vp));
        stubObtenerProducto(10L, producto(10L, true, 2, 100.0));

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.crearVenta(venta));
        assertTrue(ex.getMessage().contains("Stock insuficiente"));
        verify(ventaRepository, never()).save(any(Venta.class));
        verify(productosWebClient, never()).patch();
    }

    @Test
    void crearVenta_whenCantidadInvalida_debeFallarSinConsultarProductos() {
        // Arrange
        Venta venta = new Venta();
        venta.setClienteId(1L);
        VentaProducto vp = new VentaProducto();
        vp.setProductoId(10L);
        vp.setCantidad(0);
        venta.setProductos(List.of(vp));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> service.crearVenta(venta));
        verify(productosWebClient, never()).get();
        verify(ventaRepository, never()).save(any(Venta.class));
    }

    @Test
    void crearVenta_whenPedidoNoExiste_debeFallarSinGuardar() {
        // Arrange
        Venta venta = new Venta();
        venta.setClienteId(1L);
        venta.setPedidoId(7L);
        when(pedidosWebClient.get()).thenReturn(pedidosRequestHeadersUriSpec);
        when(pedidosRequestHeadersUriSpec.uri("/pedidos/{id}", 7L)).thenReturn(pedidosRequestHeadersSpec);
        when(pedidosRequestHeadersSpec.retrieve()).thenReturn(pedidosResponseSpec);
        when(pedidosResponseSpec.bodyToMono(Object.class)).thenReturn(Mono.error(
                WebClientResponseException.create(404, "Not Found", null, null, null)));

        // Act + Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.crearVenta(venta));
        assertTrue(ex.getMessage().contains("Pedido no encontrado"));
        verify(ventaRepository, never()).save(any(Venta.class));
        verify(productosWebClient, never()).get();
    }

    @Test
    void crearVenta_conPedidoValido_debeGuardar() {
        // Arrange
        Venta venta = new Venta();
        venta.setClienteId(1L);
        venta.setPedidoId(7L);
        stubPedidoExiste(7L);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Venta actual = service.crearVenta(venta);

        // Assert
        assertNotNull(actual);
        assertEquals(7L, actual.getPedidoId());
        verify(ventaRepository).save(any(Venta.class));
    }

    @Test
    void crearVenta_sinProductos_debeGuardarDirectamente() {
        // Arrange
        Venta venta = new Venta();
        venta.setClienteId(1L);
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Venta actual = service.crearVenta(venta);

        // Assert
        assertNotNull(actual);
        assertEquals(LocalDate.now(), actual.getFecha());
        assertEquals(0.0, actual.getTotal());
        verify(ventaRepository).save(any(Venta.class));
        verify(ventaProductoRepository, never()).saveAll(anyList());
        verify(productosWebClient, never()).patch();
    }

    @Test
    void obtenerVentaCompleta_whenExists_shouldReturnEnriched() {
        // Arrange
        Venta venta = new Venta();
        venta.setId(1L);
        VentaProducto vp = new VentaProducto();
        vp.setProductoId(10L);
        venta.setProductos(List.of(vp));
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));

        Map<String, Object> producto = producto(10L, true, 5, 100.0);
        stubObtenerProducto(10L, producto);

        // Act
        Venta actual = service.obtenerVentaCompleta(1L);

        // Assert
        assertNotNull(actual);
        assertEquals(producto, actual.getProductos().get(0).getDatosProducto());
    }

    @Test
    void obtenerVentaCompleta_whenNotExists_shouldReturnNull() {
        // Arrange
        when(ventaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertNull(service.obtenerVentaCompleta(99L));
    }

    @Test
    void resumenPorMetodoPago_shouldReturnMappedResults() {
        // Arrange
        Object[] fila1 = {"efectivo", 5L, 1500.0};
        Object[] fila2 = {"tarjeta", 3L, 900.0};
        when(ventaRepository.resumenPorMetodoPago()).thenReturn(List.of(fila1, fila2));

        // Act
        List<Map<String, Object>> actual = service.resumenPorMetodoPago();

        // Assert
        assertEquals(2, actual.size());
        assertEquals("efectivo", actual.get(0).get("metodoPago"));
        assertEquals(5L, actual.get(0).get("cantidadVentas"));
        assertEquals(1500.0, actual.get(0).get("totalRecaudado"));
        verify(ventaRepository).resumenPorMetodoPago();
    }

    private Map<String, Object> producto(Long id, boolean activo, int stock, double precio) {
        return Map.of(
                "id", id,
                "activo", activo,
                "stockDisponible", stock,
                "precioVenta", precio);
    }

    private void stubObtenerProducto(Long id, Object producto) {
        when(productosWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/productos/{id}", id)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Object.class)).thenReturn(Mono.just(producto));
    }

    private void stubDescontarStock(Long id) {
        when(productosWebClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/productos/{id}/stock", id)).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).bodyValue(any(Map.class));
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());
    }

    private void stubPedidoExiste(Long id) {
        when(pedidosWebClient.get()).thenReturn(pedidosRequestHeadersUriSpec);
        when(pedidosRequestHeadersUriSpec.uri("/pedidos/{id}", id)).thenReturn(pedidosRequestHeadersSpec);
        when(pedidosRequestHeadersSpec.retrieve()).thenReturn(pedidosResponseSpec);
        when(pedidosResponseSpec.bodyToMono(Object.class)).thenReturn(Mono.just(Map.of("id", id)));
    }
}
