package com.example.pedidos.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.example.pedidos.model.Pedido;
import com.example.pedidos.repository.PedidoRepository;

@Service
public class PedidoService {

    private final PedidoRepository repository;

    public PedidoService(PedidoRepository repository) {
        this.repository = repository;
    }

    private static final Map<String, Set<String>> TRANSICIONES_VALIDAS = Map.of(
        "pendiente",          Set.of("en_fabricacion", "cancelado"),
        "en_fabricacion",     Set.of("listo_para_entrega", "cancelado"),
        "listo_para_entrega", Set.of("entregado")
    );

    public List<Pedido> listarTodos() {
        return repository.findAll();
    }

    public Optional<Pedido> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public boolean existePorId(Long id) {
        return repository.existsById(id);
    }

    public Pedido guardar(Pedido pedido) {
        validarMontos(pedido);
        validarFechas(pedido);
        return repository.save(pedido);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    public List<Pedido> listarPorEstado(String estado) {
        return repository.findByEstado(estado);
    }

    public List<Pedido> listarPorCliente(Long clienteId) {
        return repository.findByClienteId(clienteId);
    }

    private void validarMontos(Pedido pedido) {
        BigDecimal total = pedido.getTotal() != null ? pedido.getTotal() : BigDecimal.ZERO;
        BigDecimal abono = pedido.getAbonoPagado() != null ? pedido.getAbonoPagado() : BigDecimal.ZERO;
        if (abono.compareTo(total) > 0) {
            throw new IllegalArgumentException("El abono pagado no puede ser mayor que el total del pedido");
        }
        pedido.setTotal(total);
        pedido.setAbonoPagado(abono);
    }

    private void validarFechas(Pedido pedido) {
        LocalDate fechaPedido = pedido.getFechaPedido() != null ? pedido.getFechaPedido() : LocalDate.now();
        if (pedido.getFechaEntrega() != null && pedido.getFechaEntrega().isBefore(fechaPedido)) {
            throw new IllegalArgumentException("La fecha de entrega no puede ser anterior a la fecha del pedido");
        }
        pedido.setFechaPedido(fechaPedido);
    }

    public ResponseEntity<Object> cambiarEstado(Long id, String nuevoEstado) {
        return repository.findById(id).map(pedido -> {
            String estadoActual = pedido.getEstado();
            Set<String> permitidos = TRANSICIONES_VALIDAS.get(estadoActual);

            if (permitidos == null || !permitidos.contains(nuevoEstado)) {
                String destinos = permitidos == null || permitidos.isEmpty()
                        ? "ninguno (ya está completado o cancelado)"
                        : String.join(", ", permitidos);
                return ResponseEntity
                    .badRequest()
                    .body((Object) ("Transición inválida: " + estadoActual + " → " + nuevoEstado
                        + ". Solo se permite: " + estadoActual + " → " + destinos));
            }

            pedido.setEstado(nuevoEstado);
            return ResponseEntity.ok((Object) repository.save(pedido));
        }).orElse(ResponseEntity.notFound().build());
    }
}
