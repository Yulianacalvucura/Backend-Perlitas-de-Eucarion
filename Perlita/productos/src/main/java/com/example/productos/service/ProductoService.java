package com.example.productos.service;

import com.example.productos.exception.ServicioNoDisponibleException;
import com.example.productos.model.Producto;
import com.example.productos.model.ProductoMaterial;
import com.example.productos.repository.ProductoMaterialRepository;
import com.example.productos.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoMaterialRepository productoMaterialRepository;

    @Autowired
    @Qualifier("materialesWebClient")
    private WebClient materialesWebClient;

    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    public List<Producto> listarActivos() {
        return productoRepository.findByActivoTrue();
    }

    public Optional<Producto> buscarPorId(Long id) {
        return productoRepository.findById(id);
    }

    public Producto guardar(Producto producto) {
        return productoRepository.save(producto);
    }

    public void eliminar(Long id) {
        productoRepository.deleteById(id);
    }

    public List<ProductoMaterial> obtenerMateriales(Long productoId) {
        List<ProductoMaterial> materiales = productoMaterialRepository
                .findByProductoId(productoId);
        materiales.forEach(pm -> {
            try {
                Object material = materialesWebClient.get()
                        .uri("/materiales/{id}", pm.getMaterialId())
                        .retrieve()
                        .bodyToMono(Object.class)
                        .block();
                pm.setDatosMaterial(material);
            } catch (WebClientResponseException.NotFound e) {
                pm.setDatosMaterial("Material no encontrado");
            } catch (WebClientResponseException | WebClientRequestException e) {
                throw new ServicioNoDisponibleException("Servicio de materiales no disponible", e);
            }
        });
        return materiales;
    }

    public ProductoMaterial agregarMaterial(ProductoMaterial pm) {
        return productoMaterialRepository.save(pm);
    }
}