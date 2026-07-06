package com.iptv.wiseplayer.security;

/**
 * Represents the four CRUD operations.
 * Used with {@link RequiresCrud} to declare which permission a service method requires.
 */
public enum CrudOperation {
    CREATE,
    READ,
    UPDATE,
    DELETE
}
