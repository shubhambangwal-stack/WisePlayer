package com.iptv.wiseplayer.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-level annotation that declares which CRUD permission is required
 * to execute this service method.
 *
 * <p>Usage:</p>
 * <pre>
 *   {@literal @}RequiresCrud(CrudOperation.CREATE)
 *   public Admin createSubReseller(...) { ... }
 * </pre>
 *
 * <p>Enforcement is handled centrally by {@link CrudPermissionAspect}.
 * SuperAdmin is always permitted. All other callers are checked against
 * their {@code canCreate / canRead / canUpdate / canDelete} flag.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresCrud {
    CrudOperation value();
}
