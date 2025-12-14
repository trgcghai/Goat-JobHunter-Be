package iuh.fit.goat.service.impl;

import iuh.fit.goat.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {
//    private final StringRedisTemplate redisTemplate;
//
//    @Override
//    public void saveWithTTL(String key, String value, long ttl, TimeUnit timeUnit) {
//        this.redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
//    }
//
//    @Override
//    public boolean hasKey(String key) {
//        return this.redisTemplate.hasKey(key);
//    }
//
//    @Override
//    public String getValue(String key) {
//        return this.redisTemplate.opsForValue().get(key);
//    }
//
//    @Override
//    public void deleteKey(String key) {
//        this.redisTemplate.delete(key);
//    }
//
//    @Override
//    public void replaceKey(String oldKey, String newKey, String value, long ttl, TimeUnit timeUnit) {
//        this.redisTemplate.delete(oldKey);
//        this.redisTemplate.opsForValue().set(newKey, value, ttl, timeUnit);
//    }
//
//    @Override
//    public void updateValue(String key, String value) {
//        this.redisTemplate.execute((RedisCallback<Void>) connection -> {
//            connection.stringCommands().set(
//                    key.getBytes(),
//                    value.getBytes(),
//                    Expiration.keepTtl(),
//                    RedisStringCommands.SetOption.upsert()
//            );
//            return null;
//        });
//    }
}
