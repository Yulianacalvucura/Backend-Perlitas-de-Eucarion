package com.example.ventas.model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "ventas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Venta realizada a un cliente")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id")
    private Long clienteId;

    private String clienteNombre;
    private String clienteApellidos;
    private String clienteCorreo;
    private String calle;
    private String numeroCasa;
    private Integer regionId;
    private Integer comunaId;
    @Column(length = 1000)
    private String indicaciones;
    private String usuarioNombre;
    private String estado;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @NotBlank(message = "El método de pago es obligatorio")
    @Column(nullable = false, length = 50)
    private String metodoPago;

    @NotNull(message = "El total es obligatorio")
    @Column(nullable = false)
    private Double total;

    @Column(name = "comprobante_ref", length = 100)
    private String comprobanteRef;

    @NotNull(message = "La fecha es obligatoria")
    @Column(nullable = false)
    private LocalDate fecha;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<VentaProducto> productos;
}
