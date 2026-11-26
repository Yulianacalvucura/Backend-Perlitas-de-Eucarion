package com.example.ventas.service;

import com.example.ventas.exception.ResourceNotFoundException;
import com.example.ventas.exception.ServicioNoDisponibleException;
import com.example.ventas.model.Venta;
import com.example.ventas.model.VentaProducto;
import com.example.ventas.repository.VentaProductoRepository;
import com.example.ventas.repository.VentaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VentaService {

    private static final Logger log = LoggerFactory.getLogger(VentaService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private VentaProductoRepository ventaProductoRepository;

    @Autowired
    @Qualifier("productosWebClient")
    private WebClient productosWebClient;

    @Autowired
    @Qualifier("pedidosWebClient")
    private WebClient pedidosWebClient;

    public List<Venta> listarTodas() {
        return ventaRepository.findAll();
    }

    public Optional<Venta> buscarPorId(Long id) {
        return ventaRepository.findById(id);
    }

    public List<Venta> buscarPorCliente(Long clienteId) {
        return ventaRepository.findByClienteId(clienteId);
    }

    @Transactional
    public Venta crearVenta(Venta venta) {
        return guardarVenta(venta, "PAGADO");
    }

    @Transactional
    public Venta crearCompra(Venta venta) {
        return guardarVenta(venta, "PENDIENTE");
    }

    private Venta guardarVenta(Venta venta, String estadoInicial) {
        validarPedido(venta.getPedidoId());

        List<VentaProducto> productos = venta.getProductos();
        double total = 0;
        if (productos != null) {
            for (VentaProducto vp : productos) {
                validarProducto(vp);
                total += vp.getPrecioUnitario() * vp.getCantidad();
            }
        }

        venta.setEstado(estadoInicial);
        venta.setFecha(LocalDate.now());
        venta.setTotal(total);

        Venta guardada = ventaRepository.save(venta);
        if (productos != null && !productos.isEmpty()) {
            productos.forEach(vp -> vp.setVenta(guardada));
            ventaProductoRepository.saveAll(productos);
            descontarStock(productos);
        }

        return "PENDIENTE".equals(estadoInicial) ? guardada : enriquecerConProductos(guardada);
    }

    @Transactional
    public Venta cambiarEstado(Long id, String estado) {
        if (estado == null || !Set.of("PENDIENTE", "PAGADO", "EN_PREPARACION", "ENTREGADO", "CANCELADO").contains(estado)) {
            throw new IllegalArgumentException("Estado de venta inválido");
        }
        Venta venta = ventaRepository.buscarParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con id: " + id));

        if ("CANCELADO".equals(venta.getEstado())) {
            if ("CANCELADO".equals(estado)) return venta;
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Una venta anulada no puede reactivarse");
        }

        if ("CANCELADO".equals(estado)) {
            devolverStock(venta.getProductos());
        }
        venta.setEstado(estado);
        return ventaRepository.save(venta);
    }

    private void devolverStock(List<VentaProducto> productos) {
        if (productos == null) return;
        List<VentaProducto> devueltos = new ArrayList<>();
        try {
            for (VentaProducto vp : productos) {
                productosWebClient.patch()
                        .uri("/productos/{id}/stock", vp.getProductoId())
                        .bodyValue(Map.of("ajuste", vp.getCantidad()))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();
                devueltos.add(vp);
            }
        } catch (WebClientResponseException | WebClientRequestException e) {
            // Compensar los ajustes confirmados si falla una reposición posterior.
            for (VentaProducto vp : devueltos) {
                try {
                    productosWebClient.patch()
                            .uri("/productos/{id}/stock", vp.getProductoId())
                            .bodyValue(Map.of("ajuste", -vp.getCantidad()))
                            .retrieve()
                            .bodyToMono(Void.class)
                            .block();
                } catch (RuntimeException ex) {
                    log.error("No se pudo compensar la reposición del producto {}", vp.getProductoId(), ex);
                }
            }
            throw new ServicioNoDisponibleException("No se pudo devolver el stock de la venta", e);
        }
    }

    public Venta obtenerVentaCompleta(Long id) {
        Venta venta = ventaRepository.findById(id).orElse(null);
        if (venta == null) return null;
        return enriquecerConProductos(venta);
    }

    private void validarPedido(Long pedidoId) {
        if (pedidoId == null) {
            return;
        }

        try {
            Object pedido = pedidosWebClient.get()
                    .uri("/pedidos/{id}", pedidoId)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            if (pedido == null) {
                throw new ResourceNotFoundException("Pedido no encontrado con id: " + pedidoId);
            }
        } catch (WebClientResponseException.NotFound e) {
            throw new ResourceNotFoundException("Pedido no encontrado con id: " + pedidoId);
        } catch (WebClientResponseException | WebClientRequestException e) {
            throw new ServicioNoDisponibleException("Servicio de pedidos no disponible", e);
        }
    }

    private void validarProducto(VentaProducto vp) {
        Long productoId = vp.getProductoId();
        if (productoId == null) {
            throw new IllegalArgumentException("El ID del producto es obligatorio");
        }
        if (vp.getCantidad() == null || vp.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }

        Map<String, Object> producto = objectMapper.convertValue(
                obtenerProducto(productoId), new TypeReference<Map<String, Object>>() {});

        if (!Boolean.TRUE.equals(producto.get("activo"))) {
            throw new IllegalArgumentException("El producto " + productoId + " no está activo");
        }

        Number stock = (Number) producto.get("stockDisponible");
        if (stock == null || stock.intValue() < vp.getCantidad()) {
            throw new IllegalArgumentException("Stock insuficiente para el producto " + productoId);
        }

        Number precio = (Number) producto.get("precioVenta");
        if (precio == null) {
            throw new IllegalArgumentException("El producto " + productoId + " no tiene precio de venta");
        }
        vp.setPrecioUnitario(precio.doubleValue());
        vp.setDatosProducto(producto);
    }

    private Object obtenerProducto(Long productoId) {
        try {
            return productosWebClient.get()
                    .uri("/productos/{id}", productoId)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            throw new ResourceNotFoundException("Producto no encontrado con id: " + productoId);
        } catch (WebClientResponseException | WebClientRequestException e) {
            throw new ServicioNoDisponibleException("Servicio de productos no disponible", e);
        }
    }

    private void descontarStock(List<VentaProducto> productos) {
        List<VentaProducto> descontados = new ArrayList<>();
        try {
            for (VentaProducto vp : productos) {
                productosWebClient.patch()
                        .uri("/productos/{id}/stock", vp.getProductoId())
                        .bodyValue(Map.of("ajuste", -vp.getCantidad()))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();
                descontados.add(vp);
            }
        } catch (WebClientResponseException | WebClientRequestException e) {
            restaurarStock(descontados);
            throw new ServicioNoDisponibleException("No se pudo descontar el stock de los productos", e);
        }
    }

    private void restaurarStock(List<VentaProducto> descontados) {
        for (VentaProducto vp : descontados) {
            try {
                productosWebClient.patch()
                        .uri("/productos/{id}/stock", vp.getProductoId())
                        .bodyValue(Map.of("ajuste", vp.getCantidad()))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();
            } catch (RuntimeException ex) {
                log.warn("No se pudo restaurar el stock del producto {}", vp.getProductoId(), ex);
            }
        }
    }

    private Venta enriquecerConProductos(Venta venta) {
        if (venta.getProductos() == null) return venta;

        venta.getProductos().forEach(vp -> {
            try {
                vp.setDatosProducto(obtenerProducto(vp.getProductoId()));
            } catch (ResourceNotFoundException e) {
                vp.setDatosProducto("Producto no encontrado");
            }
        });

        return venta;
    }

    public List<Map<String, Object>> resumenPorMetodoPago() {
        List<Object[]> raw = ventaRepository.resumenPorMetodoPago();
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Object[] fila : raw) {
            Map<String, Object> item = new HashMap<>();
            item.put("metodoPago", fila[0]);
            item.put("cantidadVentas", fila[1]);
            item.put("totalRecaudado", fila[2]);
            resultado.add(item);
        }
        return resultado;
    }
}
