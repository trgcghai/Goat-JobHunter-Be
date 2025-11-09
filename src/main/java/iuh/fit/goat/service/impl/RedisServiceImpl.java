package iuh.fit.goat.service.impl;

import iuh.fit.goat.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {
    private final StringRedisTemplate redisTemplate;

    @Override
    public void setTokenWithTTL(String key, String value, long ttl, TimeUnit timeUnit) {
        this.redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
    }

    @Override
    public boolean hasToken(String token) {
        return this.redisTemplate.hasKey(token);
    }

    @Override
    public void deleteToken(String key) {
        this.redisTemplate.delete(key);
    }

    @Override
    public void replaceToken(String oldToken, String newToken, String value, long ttl, TimeUnit timeUnit) {
        this.redisTemplate.delete("refresh:" + oldToken);
        this.redisTemplate.opsForValue().set("refresh:" + newToken, value, ttl, timeUnit);
    }
}
