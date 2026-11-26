package com.example.service_calendario.service;

import com.example.service_calendario.exception.ResourceNotFoundException;
import com.example.service_calendario.model.Bloqueo;
import com.example.service_calendario.model.Produccion;
import com.example.service_calendario.repository.BloqueoRepository;
import com.example.service_calendario.repository.ProduccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ProduccionServiceTest {

    private static final String URI_CAMBIAR_ESTADO_PEDIDO =
            "/pedidos/{id}/estado?nuevoEstado=listo_para_entrega";

    @Mock
    private ProduccionRepository produccionRepository;

    @Mock
    private BloqueoRepository bloqueoRepository;

    @Mock
    private WebClient productosWebClient;

    @Mock
    private WebClient pedidosWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec getResponseSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec patchResponseSpec;

    private ProduccionService service;

    @BeforeEach
    void setUp() {
        service = new ProduccionService(
                produccionRepository,
                bloqueoRepository,
                productosWebClient,
                pedidosWebClient);
    }

    @Test
    void cambiarEstado_aFinalizado_debeMarcarPedidoEnFabricacionComoListoParaEntrega() {
        Produccion produccion = produccion(1, 7, "en_proceso");
        when(produccionRepository.findById(1)).thenReturn(Optional.of(produccion));
        when(produccionRepository.save(any(Produccion.class))).thenAnswer(inv -> inv.getArgument(0));
        stubObtenerPedido(Map.of("estado", "en_fabricacion"));
        stubMarcarPedidoListo(Mono.empty());

        Produccion actual = service.cambiarEstado(1, "finalizado");

        assertEquals("finalizado", actual.getEstado());
        assertEquals(LocalDate.now(), actual.getFechaFinReal());
        verify(produccionRepository).save(any(Produccion.class));
        verify(pedidosWebClient).get();
        verify(pedidosWebClient).patch();
        verify(requestBodyUriSpec).uri(URI_CAMBIAR_ESTADO_PEDIDO, 7);
    }

    @Test
    void cambiarEstado_aFinalizado_sinPedidoId_noDebeConsultarPedidos() {
        Produccion produccion = produccion(1, null, "en_proceso");
        when(produccionRepository.findById(1)).thenReturn(Optional.of(produccion));
        when(produccionRepository.save(any(Produccion.class))).thenAnswer(inv -> inv.getArgument(0));

        Produccion actual = service.cambiarEstado(1, "finalizado");

        assertEquals("finalizado", actual.getEstado());
        verify(produccionRepository).save(any(Produccion.class));
        verify(pedidosWebClient, never()).get();
        verify(pedidosWebClient, never()).patch();
    }

    @Test
    void cambiarEstado_aFinalizado_conPedidoPendiente_noDebeCambiarEstadoDelPedido() {
        Produccion produccion = produccion(1, 7, "en_proceso");
        when(produccionRepository.findById(1)).thenReturn(Optional.of(produccion));
        when(produccionRepository.save(any(Produccion.class))).thenAnswer(inv -> inv.getArgument(0));
        stubObtenerPedido(Map.of("estado", "pendiente"));

        Produccion actual = service.cambiarEstado(1, "finalizado");

        assertEquals("finalizado", actual.getEstado());
        verify(pedidosWebClient).get();
        verify(pedidosWebClient, never()).patch();
    }

    @Test
    void cambiarEstado_aFinalizado_cuandoFallaPedidos_debeConservarProduccionFinalizada() {
        Produccion produccion = produccion(1, 7, "en_proceso");
        when(produccionRepository.findById(1)).thenReturn(Optional.of(produccion));
        when(produccionRepository.save(any(Produccion.class))).thenAnswer(inv -> inv.getArgument(0));
        stubObtenerPedido(Map.of("estado", "en_fabricacion"));
        stubMarcarPedidoListo(Mono.error(
                WebClientResponseException.create(400, "Bad Request", null, null, null)));

        Produccion actual = service.cambiarEstado(1, "finalizado");

        assertEquals("finalizado", actual.getEstado());
        assertNotNull(actual.getFechaFinReal());
        verify(produccionRepository).save(any(Produccion.class));
        verify(pedidosWebClient).patch();
    }

    @Test
    void actualizar_aFinalizado_debeMarcarPedidoEnFabricacionComoListoParaEntrega() {
        Produccion datos = produccion(1, 7, "finalizado");
        when(produccionRepository.findById(1)).thenReturn(Optional.of(produccion(1, 7, "en_proceso")));
        when(produccionRepository.save(any(Produccion.class))).thenAnswer(inv -> inv.getArgument(0));
        stubObtenerPedido(Map.of("estado", "en_fabricacion"));
        stubMarcarPedidoListo(Mono.empty());

        Produccion actual = service.actualizar(1, datos);

        assertEquals("finalizado", actual.getEstado());
        verify(produccionRepository).save(any(Produccion.class));
        verify(pedidosWebClient).patch();
    }

    @Test
    void cambiarEstado_conTransicionInvalida_debeFallarSinGuardarNiNotificar() {
        when(produccionRepository.findById(1)).thenReturn(Optional.of(produccion(1, 7, "finalizado")));

        assertThrows(IllegalArgumentException.class, () -> service.cambiarEstado(1, "en_proceso"));
        verify(produccionRepository, never()).save(any(Produccion.class));
        verify(pedidosWebClient, never()).get();
        verify(pedidosWebClient, never()).patch();
    }

    @Test
    void buscarPorId_whenNoExiste_debeLanzarResourceNotFoundException() {
        when(produccionRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.buscarPorId(99));
        verify(produccionRepository).findById(99);
    }

    @Test
    void crear_conFechaFinEstimadaAnteriorAlInicio_debeLanzarIllegalArgumentExceptionSinGuardar() {
        // Arrange
        Produccion produccion = produccion(1, 7, "programado");
        produccion.setFechaFinEstimada(LocalDate.of(2026, 6, 30));

        // Act
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> service.crear(produccion));

        // Assert
        assertEquals("La fecha fin estimada no puede ser anterior a la fecha de inicio",
                excepcion.getMessage());
        verify(produccionRepository, never()).save(any(Produccion.class));
        verify(bloqueoRepository, never()).findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                any(), any());
    }

    @Test
    void crear_conBloqueoEnElRango_debeLanzarIllegalArgumentExceptionSinGuardar() {
        // Arrange
        Produccion produccion = produccion(1, 7, "programado");
        Bloqueo bloqueo = new Bloqueo();
        bloqueo.setFechaInicio(LocalDate.of(2026, 7, 5));
        bloqueo.setFechaFin(LocalDate.of(2026, 7, 6));
        bloqueo.setMotivo("Mantenimiento");
        when(bloqueoRepository.findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 1))).thenReturn(List.of(bloqueo));

        // Act
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> service.crear(produccion));

        // Assert
        assertTrue(excepcion.getMessage().contains("existe un bloqueo en ese rango de fechas"));
        assertTrue(excepcion.getMessage().contains("Mantenimiento"));
        verify(produccionRepository, never()).save(any(Produccion.class));
    }

    @Test
    void crear_sinBloqueoEnElRango_debeGuardarProduccion() {
        // Arrange
        Produccion produccion = produccion(1, 7, "programado");
        when(bloqueoRepository.findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 1))).thenReturn(List.of());
        when(produccionRepository.save(produccion)).thenReturn(produccion);

        // Act
        Produccion guardada = service.crear(produccion);

        // Assert
        assertSame(produccion, guardada);
        verify(produccionRepository).save(produccion);
    }

    @Test
    void cambiarEstado_deProgramadoAEnProceso_debeGuardarSinAsignarFechaFinRealNiNotificar() {
        // Arrange
        Produccion produccion = produccion(1, 7, "programado");
        when(produccionRepository.findById(1)).thenReturn(Optional.of(produccion));
        when(produccionRepository.save(any(Produccion.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Produccion actual = service.cambiarEstado(1, "en_proceso");

        // Assert
        assertEquals("en_proceso", actual.getEstado());
        assertNull(actual.getFechaFinReal());
        verify(produccionRepository).save(any(Produccion.class));
        verify(pedidosWebClient, never()).get();
        verify(pedidosWebClient, never()).patch();
    }

    @Test
    void cambiarEstado_deEnProcesoACancelado_debeGuardarSinNotificarPedido() {
        // Arrange
        Produccion produccion = produccion(1, 7, "en_proceso");
        when(produccionRepository.findById(1)).thenReturn(Optional.of(produccion));
        when(produccionRepository.save(any(Produccion.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Produccion actual = service.cambiarEstado(1, "cancelado");

        // Assert
        assertEquals("cancelado", actual.getEstado());
        assertNull(actual.getFechaFinReal());
        verify(produccionRepository).save(any(Produccion.class));
        verify(pedidosWebClient, never()).get();
        verify(pedidosWebClient, never()).patch();
    }

    @Test
    void cambiarEstado_desdeCancelado_debeLanzarIllegalArgumentExceptionSinGuardarNiNotificar() {
        // Arrange
        when(produccionRepository.findById(1)).thenReturn(Optional.of(produccion(1, 7, "cancelado")));

        // Act
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> service.cambiarEstado(1, "programado"));

        // Assert
        assertTrue(excepcion.getMessage().contains("Transición inválida: cancelado → programado"));
        verify(produccionRepository, never()).save(any(Produccion.class));
        verify(pedidosWebClient, never()).get();
        verify(pedidosWebClient, never()).patch();
    }

    private void stubObtenerPedido(Map<String, Object> pedido) {
        when(pedidosWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/pedidos/{id}", 7)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        doReturn(Mono.just(pedido)).when(getResponseSpec).bodyToMono(Map.class);
    }

    private void stubMarcarPedidoListo(Mono<?> respuesta) {
        when(pedidosWebClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(URI_CAMBIAR_ESTADO_PEDIDO, 7)).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(patchResponseSpec);
        doReturn(respuesta).when(patchResponseSpec).bodyToMono(Object.class);
    }

    private Produccion produccion(Integer id, Integer pedidoId, String estado) {
        Produccion produccion = new Produccion();
        produccion.setId(id);
        produccion.setPedidoId(pedidoId);
        produccion.setProductoId(5);
        produccion.setFechaInicio(LocalDate.of(2026, 7, 1));
        produccion.setFechaFinEstimada(LocalDate.of(2026, 7, 10));
        produccion.setEstado(estado);
        return produccion;
    }
}
