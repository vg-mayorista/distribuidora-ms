package com.distribuidora.exception;

/**
 * Thrown when a category name already exists in the catalog.
 * Case-sensitive uniqueness check per BR-001.
 */
public class DuplicateCategoryException extends RuntimeException {

    private final String name;

    public DuplicateCategoryException(String name) {
        super("Category already exists with name: '" + name + "'");
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
