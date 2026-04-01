package iuh.fit.goat.util;

import org.hibernate.proxy.HibernateProxy;

/**
 * Utility to unwrap Hibernate proxies to actual entity implementations.
 */
public final class EntityUtil {
    private EntityUtil() {}

    @SuppressWarnings("unchecked")
    public static <T> T unproxy(Object entity) {
        if (entity instanceof HibernateProxy) {
            return (T) ((HibernateProxy) entity).getHibernateLazyInitializer().getImplementation();
        }
        return (T) entity;
    }
}

