package com.example.service_calendario.service;

import com.example.service_calendario.exception.ResourceNotFoundException;
import com.example.service_calendario.model.Bloqueo;
import com.example.service_calendario.repository.BloqueoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloqueoServiceTest {

    @Mock
    private BloqueoRepository bloqueoRepository;

    private BloqueoService service;

    @BeforeEach
    void setUp() {
        service = new BloqueoService(bloqueoRepository);
    }

    @Test
    void listarTodos_cuandoHayBloqueos_debeDevolverTodos() {
        // Arrange
        List<Bloqueo> bloqueos = List.of(
                bloqueo(1, LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 31), "Vacaciones"),
                bloqueo(2, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 2), "Año nuevo"));
        when(bloqueoRepository.findAll()).thenReturn(bloqueos);

        // Act
        List<Bloqueo> resultado = service.listarTodos();

        // Assert
        assertSame(bloqueos, resultado);
        assertEquals(2, resultado.size());
        verify(bloqueoRepository).findAll();
    }

    @Test
    void crear_conRangoValido_debeGuardarBloqueo() {
        // Arrange
        Bloqueo bloqueo = bloqueo(null, LocalDate.of(2026, 12, 24),
                LocalDate.of(2026, 12, 31), "Vacaciones de fin de año");
        when(bloqueoRepository.save(bloqueo)).thenReturn(bloqueo);

        // Act
        Bloqueo guardado = service.crear(bloqueo);

        // Assert
        assertSame(bloqueo, guardado);
        verify(bloqueoRepository).save(bloqueo);
    }

    @Test
    void crear_conFechaFinAnteriorAlInicio_debeLanzarIllegalArgumentExceptionSinGuardar() {
        // Arrange
        Bloqueo bloqueo = bloqueo(null, LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 12, 24), "Rango inválido");

        // Act
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> service.crear(bloqueo));

        // Assert
        assertEquals("La fecha fin no puede ser anterior a la fecha de inicio", excepcion.getMessage());
        verify(bloqueoRepository, never()).save(any(Bloqueo.class));
    }

    @Test
    void eliminar_cuandoExiste_debeBorrarBloqueo() {
        // Arrange
        when(bloqueoRepository.existsById(1)).thenReturn(true);

        // Act
        service.eliminar(1);

        // Assert
        verify(bloqueoRepository).deleteById(1);
    }

    @Test
    void eliminar_cuandoNoExiste_debeLanzarResourceNotFoundExceptionSinBorrar() {
        // Arrange
        when(bloqueoRepository.existsById(99)).thenReturn(false);

        // Act
        ResourceNotFoundException excepcion = assertThrows(
                ResourceNotFoundException.class,
                () -> service.eliminar(99));

        // Assert
        assertTrue(excepcion.getMessage().contains("Bloqueo no encontrado con id: 99"));
        verify(bloqueoRepository, never()).deleteById(any());
    }

    private Bloqueo bloqueo(Integer id, LocalDate inicio, LocalDate fin, String motivo) {
        Bloqueo bloqueo = new Bloqueo();
        bloqueo.setId(id);
        bloqueo.setFechaInicio(inicio);
        bloqueo.setFechaFin(fin);
        bloqueo.setMotivo(motivo);
        return bloqueo;
    }
}
