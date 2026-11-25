package com.example.productos.controller;

import com.example.productos.exception.ResourceNotFoundException;
import com.example.productos.model.Producto;
import com.example.productos.model.ProductoMaterial;
import com.example.productos.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/productos")
@Tag(name = "Productos", description = "Gestión de productos")
public class ProductoController {

    @Autowired
    private ProductoService service;

    @Operation(summary = "Listar todos los productos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de productos obtenida correctamente"),
            @ApiResponse(responseCode = "401", description = "Token JWT faltante o inválido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para consultar productos")
    })
    @GetMapping
    public List<Producto> listar() {
        return service.listarTodos();
    }

    @Operation(summary = "Listar productos activos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Productos activos obtenidos correctamente"),
            @ApiResponse(responseCode = "401", description = "Token JWT faltante o inválido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para consultar productos")
    })
    @GetMapping("/activos")
    public List<Producto> listarActivos() {
        return service.listarActivos();
    }

    @Operation(summary = "Buscar producto por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "401", description = "Token JWT faltante o inválido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para esta operación"),
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Producto> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
    }

    @Operation(summary = "Crear un producto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Producto creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "401", description = "Token JWT faltante o inválido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para esta operación")
    })
    @PostMapping
    public ResponseEntity<Producto> crear(@Valid @RequestBody Producto producto) {
        return new ResponseEntity<>(service.guardar(producto), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar un producto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "401", description = "Token JWT faltante o inválido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para esta operación"),
            @ApiResponse(responseCode = "200", description = "Producto actualizado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(@PathVariable Long id,
                                                @Valid @RequestBody Producto producto) {
        service.buscarPorId(id).orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        producto.setId(id);
        return ResponseEntity.ok(service.guardar(producto));
    }

    @Operation(summary = "Ajustar el stock disponible de un producto",
               description = "El campo 'ajuste' es un delta: negativo descuenta y positivo repone.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "401", description = "Token JWT faltante o inválido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para esta operación"),
            @ApiResponse(responseCode = "200", description = "Stock actualizado"),
            @ApiResponse(responseCode = "400", description = "Ajuste inválido o stock insuficiente"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @PatchMapping("/{id}/stock")
    public ResponseEntity<Producto> ajustarStock(@PathVariable Long id,
                                                 @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(service.ajustarStock(id, body.get("ajuste")));
    }

    @Operation(summary = "Eliminar un producto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "401", description = "Token JWT faltante o inválido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para esta operación"),
            @ApiResponse(responseCode = "204", description = "Producto eliminado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.buscarPorId(id).orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ver materiales de un producto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Materiales del producto obtenidos correctamente"),
            @ApiResponse(responseCode = "401", description = "Token JWT faltante o inválido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para consultar productos"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @GetMapping("/{id}/materiales")
    public List<ProductoMaterial> verMateriales(@PathVariable Long id) {
        return service.obtenerMateriales(id);
    }

    @Operation(summary = "Agregar material a un producto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Material agregado al producto"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "401", description = "Token JWT faltante o inválido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para esta operación")
    })
    @PostMapping("/{id}/materiales")
    public ResponseEntity<ProductoMaterial> agregarMaterial(
            @PathVariable Long id,
            @Valid @RequestBody ProductoMaterial pm) {
        return service.buscarPorId(id)
                .map(producto -> {
                    pm.setProducto(producto);
                    return new ResponseEntity<>(service.agregarMaterial(pm), HttpStatus.CREATED);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
    }
}
