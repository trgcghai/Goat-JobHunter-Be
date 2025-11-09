package iuh.fit.goat.service;

import java.util.concurrent.TimeUnit;

public interface RedisService {
    void setTokenWithTTL(String key, String value, long ttl, TimeUnit timeUnit);

    boolean hasToken(String token);

    void deleteToken(String key);

    void replaceToken(String oldToken, String newToken, String value, long ttl, TimeUnit timeUnit);
}
