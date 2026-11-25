package com.example.service_calendario.service;

import com.example.service_calendario.exception.ResourceNotFoundException;
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
        // Arrange
        Produccion produccion = produccion(1, 7, "en_proceso");
        when(produccionRepository.findById(1)).thenReturn(Optional.of(produccion));
        when(produccionRepository.save(any(Produccion.class))).thenAnswer(inv -> inv.getArgument(0));
        stubObtenerPedido(Map.of("estado", "en_fabricacion"));
        stubMarcarPedidoListo(Mono.empty());

        // Act
        Produccion actual = service.cambiarEstado(1, "finalizado");

        // Assert
        assertEquals("finalizado", actual.getEstado());
        assertEquals(LocalDate.now(), actual.getFechaFinReal());
        verify(produccionRepository).save(any(Produccion.class));
        verify(pedidosWebClient).get();
        verify(pedidosWebClient).patch();
        verify(requestBodyUriSpec).uri(URI_CAMBIAR_ESTADO_PEDIDO, 7);
    }

    @Test
    void cambiarEstado_aFinalizado_sinPedidoId_noDebeConsultarPedidos() {
        // Arrange
        Produccion produccion = produccion(1, null, "en_proceso");
        when(produccionRepository.findById(1)).thenReturn(Optional.of(produccion));
        when(produccionRepository.save(any(Produccion.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Produccion actual = service.cambiarEstado(1, "finalizado");

        // Assert
        assertEquals("finalizado", actual.getEstado());
        verify(produccionRepository).save(any(Produccion.class));
        verify(pedidosWebClient, never()).get();
        verify(pedidosWebClient, never()).patch();
    }

    @Test
    void cambiarEstado_aFinalizado_conPedidoPendiente_noDebeCambiarEstadoDelPedido() {
        // Arrange
        Produccion produccion = produccion(1, 7, "en_proceso");
        when(produccionRepository.findById(1)).thenReturn(Optional.of(produccion));
        when(produccionRepository.save(any(Produccion.class))).thenAnswer(inv -> inv.getArgument(0));
        stubObtenerPedido(Map.of("estado", "pendiente"));

        // Act
        Produccion actual = service.cambiarEstado(1, "finalizado");

        // Assert
        assertEquals("finalizado", actual.getEstado());
        verify(pedidosWebClient).get();
        verify(pedidosWebClient, never()).patch();
    }

    @Test
    void cambiarEstado_aFinalizado_cuandoFallaPedidos_debeConservarProduccionFinalizada() {
        // Arrange
        Produccion produccion = produccion(1, 7, "en_proceso");
        when(produccionRepository.findById(1)).thenReturn(Optional.of(produccion));
        when(produccionRepository.save(any(Produccion.class))).thenAnswer(inv -> inv.getArgument(0));
        stubObtenerPedido(Map.of("estado", "en_fabricacion"));
        stubMarcarPedidoListo(Mono.error(
                WebClientResponseException.create(400, "Bad Request", null, null, null)));

        // Act
        Produccion actual = service.cambiarEstado(1, "finalizado");

        // Assert
        assertEquals("finalizado", actual.getEstado());
        assertNotNull(actual.getFechaFinReal());
        verify(produccionRepository).save(any(Produccion.class));
        verify(pedidosWebClient).patch();
    }

    @Test
    void actualizar_aFinalizado_debeMarcarPedidoEnFabricacionComoListoParaEntrega() {
        // Arrange
        Produccion datos = produccion(1, 7, "finalizado");
        when(produccionRepository.findById(1)).thenReturn(Optional.of(produccion(1, 7, "en_proceso")));
        when(produccionRepository.save(any(Produccion.class))).thenAnswer(inv -> inv.getArgument(0));
        stubObtenerPedido(Map.of("estado", "en_fabricacion"));
        stubMarcarPedidoListo(Mono.empty());

        // Act
        Produccion actual = service.actualizar(1, datos);

        // Assert
        assertEquals("finalizado", actual.getEstado());
        verify(produccionRepository).save(any(Produccion.class));
        verify(pedidosWebClient).patch();
    }

    @Test
    void cambiarEstado_conTransicionInvalida_debeFallarSinGuardarNiNotificar() {
        // Arrange
        when(produccionRepository.findById(1)).thenReturn(Optional.of(produccion(1, 7, "finalizado")));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> service.cambiarEstado(1, "en_proceso"));
        verify(produccionRepository, never()).save(any(Produccion.class));
        verify(pedidosWebClient, never()).get();
        verify(pedidosWebClient, never()).patch();
    }

    @Test
    void buscarPorId_whenNoExiste_debeLanzarResourceNotFoundException() {
        // Arrange
        when(produccionRepository.findById(99)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(ResourceNotFoundException.class, () -> service.buscarPorId(99));
        verify(produccionRepository).findById(99);
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
