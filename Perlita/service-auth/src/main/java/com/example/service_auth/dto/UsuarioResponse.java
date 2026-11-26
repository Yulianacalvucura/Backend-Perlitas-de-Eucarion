package com.example.service_auth.dto;

import com.example.service_auth.model.Usuario;

public record UsuarioResponse(Long id, String nombreUsuario, String nombre,
                              String apellidos, String correo, String rol, boolean activo) {
    public static UsuarioResponse desde(Usuario usuario) {
        String rol = usuario.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getNombreRol()))
                ? "ADMIN" : "VENDEDOR";
        return new UsuarioResponse(usuario.getId(), usuario.getNombreUsuario(),
                usuario.getNombre() == null ? usuario.getNombreUsuario() : usuario.getNombre(),
                usuario.getApellidos() == null ? "" : usuario.getApellidos(),
                usuario.getCorreo(), rol, usuario.isActivo());
    }
}
