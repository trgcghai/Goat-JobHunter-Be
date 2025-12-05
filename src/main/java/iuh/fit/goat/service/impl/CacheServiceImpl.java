package iuh.fit.goat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iuh.fit.goat.service.CacheService;
import iuh.fit.goat.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {
    private final RedisService redisService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private <T> T fromJson(String json, Class<T> clazz) {
        try {
            return this.objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(Object obj) {
        try {
            return this.objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public <T> T getOrSet(String cacheName, String key, Class<T> clazz, Supplier<T> supplier, long ttlSeconds) {
        String redisKey = "cache:" + cacheName + ":" + key;

        if (this.redisService.hasKey(redisKey)) {
            String json = this.redisService.getValue(redisKey);
            return this.fromJson(json, clazz);
        }

        T value = supplier.get();
        if (value != null) {
            this.redisService.saveWithTTL(redisKey, toJson(value), ttlSeconds, TimeUnit.SECONDS);
        }

        return value;
    }

    @Override
    public void evict(String cacheName, String key) {
        this.redisService.deleteKey("cache:" + cacheName + ":" + key);
    }

    @Override
    public void clear(String cacheName) {

    }
}
