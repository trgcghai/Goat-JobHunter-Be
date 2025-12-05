package iuh.fit.goat.service;

import java.util.function.Supplier;

public interface CacheService {
    <T> T getOrSet(String cacheName, String key, Class<T> clazz, Supplier<T> supplier, long ttlSeconds);

    void evict(String cacheName, String key);

    void clear(String cacheName);
}
