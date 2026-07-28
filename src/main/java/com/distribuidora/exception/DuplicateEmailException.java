package com.distribuidora.exception;

/**
 * Excepción lanzada cuando un email ya está registrado en el sistema.
 */
public class DuplicateEmailException extends RuntimeException {

    private final String email;

    public DuplicateEmailException(String email) {
        super("El correo electrónico ya está registrado: '" + email + "'");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
