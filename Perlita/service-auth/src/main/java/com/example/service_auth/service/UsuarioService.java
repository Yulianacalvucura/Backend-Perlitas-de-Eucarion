package com.example.service_auth.service;

import com.example.service_auth.dto.UsuarioRequest;
import com.example.service_auth.dto.UsuarioResponse;
import com.example.service_auth.exception.UsuarioDuplicadoException;
import com.example.service_auth.exception.RolInvalidoException;
import com.example.service_auth.model.Rol;
import com.example.service_auth.model.Usuario;
import com.example.service_auth.repository.RolRepository;
import com.example.service_auth.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@Transactional
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarios.findAll().stream().map(UsuarioResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.desde(buscar(id));
    }

    public UsuarioResponse crear(UsuarioRequest datos) {
        if (datos.password() == null || datos.password().length() < 8 || datos.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres y máximo 72 bytes.");
        }
        validarCorreo(datos.correo(), null);
        String nombreUsuario = datos.correo().trim().toLowerCase(java.util.Locale.ROOT);
        if (usuarios.findByNombreUsuario(nombreUsuario).isPresent()) {
            throw new UsuarioDuplicadoException("El nombre de usuario ya existe.");
        }
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setContrasena(encoder.encode(datos.password()));
        aplicarDatos(usuario, datos);
        usuario.setActivo(true);
        return UsuarioResponse.desde(usuarios.save(usuario));
    }

    public UsuarioResponse actualizar(Long id, UsuarioRequest datos, String solicitante) {
        Usuario usuario = buscar(id);
        boolean propio = usuario.getNombreUsuario().equals(solicitante);
        if (propio && (Boolean.FALSE.equals(datos.activo()) || !"ADMIN".equals(datos.rol()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puede desactivar su cuenta ni quitarse el rol ADMIN.");
        }
        validarCorreo(datos.correo(), id);
        aplicarDatos(usuario, datos);
        if (datos.activo() != null) usuario.setActivo(datos.activo());
        return UsuarioResponse.desde(usuarios.save(usuario));
    }

    public void darBaja(Long id, String solicitante) {
        Usuario usuario = buscar(id);
        if (usuario.getNombreUsuario().equals(solicitante)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puede dar de baja su propia cuenta.");
        }
        usuario.setActivo(false);
        usuarios.save(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    private void validarCorreo(String correo, Long id) {
        usuarios.findByCorreoIgnoreCase(correo.trim()).ifPresent(u -> {
            if (!u.getId().equals(id)) throw new UsuarioDuplicadoException("El correo ya existe.");
        });
        usuarios.findByNombreUsuario(correo.trim()).ifPresent(u -> {
            if (!u.getId().equals(id)) throw new UsuarioDuplicadoException("El correo coincide con otro usuario.");
        });
    }

    private void aplicarDatos(Usuario usuario, UsuarioRequest datos) {
        Rol rol = roles.findByNombreRol(datos.rol()).orElseThrow(() -> new RolInvalidoException("Rol no permitido."));
        usuario.setNombre(datos.nombre().trim());
        usuario.setApellidos(datos.apellidos() == null ? "" : datos.apellidos().trim());
        usuario.setCorreo(datos.correo().trim().toLowerCase(java.util.Locale.ROOT));
        usuario.getRoles().clear();
        usuario.getRoles().add(rol);
    }
}
