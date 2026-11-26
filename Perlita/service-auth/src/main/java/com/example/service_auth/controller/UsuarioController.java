package com.example.service_auth.controller;

import com.example.service_auth.dto.UsuarioRequest;
import com.example.service_auth.dto.UsuarioResponse;
import com.example.service_auth.model.Usuario;
import com.example.service_auth.repository.UsuarioRepository;
import com.example.service_auth.service.JwtService;
import com.example.service_auth.service.UsuarioService;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/auth")
@Tag(name = "Usuarios del personal")
public class UsuarioController {
    private final UsuarioService servicio;
    private final UsuarioRepository usuarios;
    private final JwtService jwt;

    public UsuarioController(UsuarioService servicio, UsuarioRepository usuarios, JwtService jwt) {
        this.servicio = servicio;
        this.usuarios = usuarios;
        this.jwt = jwt;
    }

    @GetMapping("/me")
    public UsuarioResponse perfil(@RequestHeader(value = "Authorization", required = false) String token) {
        return UsuarioResponse.desde(autenticado(token, false));
    }

    @GetMapping("/usuarios")
    public List<UsuarioResponse> listar(@RequestHeader(value = "Authorization", required = false) String token) {
        autenticado(token, true);
        return servicio.listar();
    }

    @GetMapping("/usuarios/{id}")
    public UsuarioResponse obtener(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String token) {
        autenticado(token, true);
        return servicio.obtener(id);
    }

    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest datos,
            @RequestHeader(value = "Authorization", required = false) String token) {
        autenticado(token, true);
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(datos));
    }

    @PutMapping("/usuarios/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest datos,
            @RequestHeader(value = "Authorization", required = false) String token) {
        return servicio.actualizar(id, datos, autenticado(token, true).getNombreUsuario());
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> darBaja(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        servicio.darBaja(id, autenticado(token, true).getNombreUsuario());
        return ResponseEntity.noContent().build();
    }

    private Usuario autenticado(String token, boolean admin) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token requerido.");
        }
        String nombre;
        try {
            nombre = jwt.obtenerUsuario(token.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido o expirado.");
        }
        Usuario usuario = usuarios.findByNombreUsuario(nombre).filter(Usuario::isActivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario inactivo o inexistente."));
        if (admin && usuario.getRoles().stream().noneMatch(r -> "ADMIN".equals(r.getNombreRol()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado.");
        }
        return usuario;
    }
}
