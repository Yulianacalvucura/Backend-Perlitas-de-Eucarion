package com.example.service_auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank @Size(max = 255) String nombre,
        @Size(max = 255) String apellidos,
        @NotBlank @Email @Size(max = 255) String correo,
        String password,
        @NotBlank @Pattern(regexp = "ADMIN|VENDEDOR") String rol,
        Boolean activo) { }
