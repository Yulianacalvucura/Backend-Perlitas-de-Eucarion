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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void registrar_whenUserDoesNotExist_shouldRegister() {
        AuthRequest request = new AuthRequest();
        request.setNombreUsuario("nuevo");
        request.setContrasena("pass12345");
        request.setCorreo("nuevo@test.cl");
        request.setRoles(Set.of("ADMIN"));

        when(usuarioRepository.findByNombreUsuario("nuevo")).thenReturn(Optional.empty());

        Rol rolAdmin = new Rol();
        rolAdmin.setId(1L);
        rolAdmin.setNombreRol("ADMIN");
        when(rolRepository.findByNombreRol("ADMIN")).thenReturn(Optional.of(rolAdmin));
        when(passwordEncoder.encode("pass12345")).thenReturn("encoded");

        String result = authService.registrar(request);

        assertEquals("Usuario Registrado", result);
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void registrar_whenUserAlreadyExists_shouldThrowConflicto() {
        AuthRequest request = new AuthRequest();
        request.setNombreUsuario("existente");

        when(usuarioRepository.findByNombreUsuario("existente")).thenReturn(Optional.of(new Usuario()));

        assertThrows(UsuarioDuplicadoException.class, () -> authService.registrar(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_whenDefaultRole_shouldAssignVendedor() {
        AuthRequest request = new AuthRequest();
        request.setNombreUsuario("test");
        request.setContrasena("pass12345");
        request.setCorreo("test@test.cl");
        request.setRoles(null);

        when(usuarioRepository.findByNombreUsuario("test")).thenReturn(Optional.empty());

        Rol rolVendedor = new Rol();
        rolVendedor.setId(2L);
        rolVendedor.setNombreRol("VENDEDOR");
        when(rolRepository.findByNombreRol("VENDEDOR")).thenReturn(Optional.of(rolVendedor));
        when(passwordEncoder.encode("pass12345")).thenReturn("encoded");

        authService.registrar(request);

        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void registrar_whenRoleCliente_shouldThrowRolInvalido() {
        AuthRequest request = new AuthRequest();
        request.setNombreUsuario("cliente");
        request.setContrasena("pass12345");
        request.setCorreo("cliente@test.cl");
        request.setRoles(Set.of("CLIENTE"));

        when(usuarioRepository.findByNombreUsuario("cliente")).thenReturn(Optional.empty());

        RolInvalidoException ex = assertThrows(RolInvalidoException.class, () -> authService.registrar(request));
        assertTrue(ex.getMessage().contains("Solo se aceptan ADMIN y VENDEDOR"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void login_withValidCredentials_shouldReturnToken() {
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario("admin");
        usuario.setContrasena("encodedPass");

        Rol rol = new Rol();
        rol.setNombreRol("ADMIN");
        usuario.setRoles(Set.of(rol));

        when(usuarioRepository.findByNombreUsuario("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("pass12345", "encodedPass")).thenReturn(true);
        when(jwtService.generarToken("admin", List.of("ADMIN"))).thenReturn("token123");

        String token = authService.login("admin", "pass12345");

        assertEquals("token123", token);
        verify(jwtService).generarToken("admin", List.of("ADMIN"));
    }

    @Test
    void login_withInvalidPassword_shouldThrowNoAutorizado() {
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario("admin");
        usuario.setContrasena("encodedPass");

        when(usuarioRepository.findByNombreUsuario("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrong", "encodedPass")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> authService.login("admin", "wrong"));
    }

    @Test
    void login_withNonExistentUser_shouldThrowNoAutorizado() {
        when(usuarioRepository.findByNombreUsuario("ghost")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> authService.login("ghost", "pass12345"));
    }
}
