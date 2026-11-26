package com.example.ventas.repository;

import com.example.ventas.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
    List<Venta> findByClienteId(Long clienteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Venta v WHERE v.id = :id")
    Optional<Venta> buscarParaActualizar(@Param("id") Long id);

    @Query("SELECT v.metodoPago, COUNT(v), SUM(v.total) FROM Venta v GROUP BY v.metodoPago")
    List<Object[]> resumenPorMetodoPago();
}