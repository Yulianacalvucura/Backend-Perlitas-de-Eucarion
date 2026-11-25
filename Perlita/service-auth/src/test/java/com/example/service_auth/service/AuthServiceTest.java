package com.example.service_auth.service;

import com.example.service_auth.dto.AuthRequest;
import com.example.service_auth.exception.CredencialesInvalidasException;
import com.example.service_auth.exception.RolInvalidoException;
import com.example.service_auth.exception.UsuarioDuplicadoException;
import com.example.service_auth.model.Rol;
import com.example.service_auth.model.Usuario;
import com.example.service_auth.repository.RolRepository;
import com.example.service_auth.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void registrar_conUsuarioNuevoYAdministrador_debeRegistrarConContrasenaCodificada() {
        // Arrange
        AuthRequest solicitud = nuevaSolicitud("nuevo", Set.of("ADMIN"));
        Rol rolAdmin = nuevoRol(1L, "ADMIN");
        when(usuarioRepository.findByNombreUsuario("nuevo")).thenReturn(Optional.empty());
        when(rolRepository.findByNombreRol("ADMIN")).thenReturn(Optional.of(rolAdmin));
        when(passwordEncoder.encode("pass12345")).thenReturn("contrasena-codificada");

        // Act
        String resultado = authService.registrar(solicitud);

        // Assert
        assertEquals("Usuario Registrado", resultado);
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertEquals("nuevo", guardado.getNombreUsuario());
        assertEquals("contrasena-codificada", guardado.getContrasena());
        assertTrue(guardado.getRoles().stream()
                .anyMatch(rol -> "ADMIN".equals(rol.getNombreRol())));
    }

    @Test
    void registrar_conNombreDuplicado_debeLanzarUsuarioDuplicadoYNoGuardar() {
        // Arrange
        AuthRequest solicitud = nuevaSolicitud("existente", Set.of("VENDEDOR"));
        when(usuarioRepository.findByNombreUsuario("existente")).thenReturn(Optional.of(new Usuario()));

        // Act
        UsuarioDuplicadoException excepcion = assertThrows(
                UsuarioDuplicadoException.class,
                () -> authService.registrar(solicitud));

        // Assert
        assertEquals("El nombre de usuario ya existe.", excepcion.getMessage());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void registrar_sinRoles_debeAsignarRolVendedorPorDefecto() {
        // Arrange
        AuthRequest solicitud = nuevaSolicitud("vendedor", null);
        Rol rolVendedor = nuevoRol(2L, "VENDEDOR");
        when(usuarioRepository.findByNombreUsuario("vendedor")).thenReturn(Optional.empty());
        when(rolRepository.findByNombreRol("VENDEDOR")).thenReturn(Optional.of(rolVendedor));
        when(passwordEncoder.encode("pass12345")).thenReturn("contrasena-codificada");

        // Act
        authService.registrar(solicitud);

        // Assert
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertEquals(1, guardado.getRoles().size());
        assertEquals("VENDEDOR", guardado.getRoles().iterator().next().getNombreRol());
        verify(rolRepository, never()).findByNombreRol("ADMIN");
    }

    @Test
    void registrar_conRolCliente_debeLanzarRolInvalidoYNoGuardar() {
        // Arrange
        AuthRequest solicitud = nuevaSolicitud("cliente", Set.of("CLIENTE"));
        when(usuarioRepository.findByNombreUsuario("cliente")).thenReturn(Optional.empty());

        // Act
        RolInvalidoException excepcion = assertThrows(
                RolInvalidoException.class,
                () -> authService.registrar(solicitud));

        // Assert
        assertTrue(excepcion.getMessage().contains("Solo se aceptan ADMIN y VENDEDOR"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(rolRepository, never()).findByNombreRol(any());
    }

    @Test
    void login_conCredencialesValidas_debeDevolverTokenConLosRolesDelUsuario() {
        // Arrange
        Usuario usuario = nuevoUsuario("admin", "contrasena-codificada", Set.of(nuevoRol(1L, "ADMIN")));
        when(usuarioRepository.findByNombreUsuario("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("pass12345", "contrasena-codificada")).thenReturn(true);
        when(jwtService.generarToken("admin", List.of("ADMIN"))).thenReturn("token123");

        // Act
        String token = authService.login("admin", "pass12345");

        // Assert
        assertEquals("token123", token);
        verify(jwtService).generarToken("admin", List.of("ADMIN"));
    }

    @Test
    void login_conContrasenaErronea_debeLanzarCredencialesInvalidas() {
        // Arrange
        Usuario usuario = nuevoUsuario("admin", "contrasena-codificada", Set.of(nuevoRol(1L, "ADMIN")));
        when(usuarioRepository.findByNombreUsuario("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", "contrasena-codificada")).thenReturn(false);

        // Act
        CredencialesInvalidasException excepcion = assertThrows(
                CredencialesInvalidasException.class,
                () -> authService.login("admin", "incorrecta"));

        // Assert
        assertEquals("Credenciales inválidas", excepcion.getMessage());
        verify(jwtService, never()).generarToken(any(), any());
    }

    @Test
    void login_conUsuarioInexistente_debeLanzarCredencialesInvalidas() {
        // Arrange
        when(usuarioRepository.findByNombreUsuario("inexistente")).thenReturn(Optional.empty());

        // Act
        CredencialesInvalidasException excepcion = assertThrows(
                CredencialesInvalidasException.class,
                () -> authService.login("inexistente", "pass12345"));

        // Assert
        assertEquals("Credenciales inválidas", excepcion.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generarToken(any(), any());
    }

    private AuthRequest nuevaSolicitud(String nombreUsuario, Set<String> roles) {
        AuthRequest solicitud = new AuthRequest();
        solicitud.setNombreUsuario(nombreUsuario);
        solicitud.setContrasena("pass12345");
        solicitud.setCorreo(nombreUsuario + "@perlitas.cl");
        solicitud.setRoles(roles);
        return solicitud;
    }

    private Rol nuevoRol(Long id, String nombre) {
        Rol rol = new Rol();
        rol.setId(id);
        rol.setNombreRol(nombre);
        return rol;
    }

    private Usuario nuevoUsuario(String nombre, String contrasena, Set<Rol> roles) {
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(nombre);
        usuario.setContrasena(contrasena);
        usuario.setRoles(roles);
        return usuario;
    }
}
