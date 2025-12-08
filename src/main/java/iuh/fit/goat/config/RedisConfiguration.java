package iuh.fit.goat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import iuh.fit.goat.config.components.RedisKeyExpirationListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {

    @Value("${spring.data.redis.host}")
    private String redisHost;
    @Value("${spring.data.redis.port}")
    private int redisPort;
    @Value("${spring.data.redis.username}")
    private String redisUsername;
    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setUsername(redisUsername);
        config.setPassword(redisPassword);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(mapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    // Ensure keyspace notifications are enabled and register listener
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory factory,
                                                                       RedisKeyExpirationListener expirationListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        // Message adapter binds incoming message payload to handler method "handleMessage"
        MessageListenerAdapter adapter = new MessageListenerAdapter(expirationListener, "handleMessage");
        container.addMessageListener(adapter, new PatternTopic("__keyevent@*__:expired"));

        // Try to enable notifications on the Redis server (best-effort).
        try {
            // Use a StringRedisTemplate to execute CONFIG SET
            StringRedisTemplate stringTemplate = new StringRedisTemplate(factory);
            stringTemplate.afterPropertiesSet();
            stringTemplate.execute((RedisCallback<Object>) connection -> {
                // serverCommands().configSet is supported by Spring's RedisConnection for many drivers
                try {
                    RedisServerCommands server = connection.serverCommands();
                    server.setConfig("notify-keyspace-events", "Ex");
                } catch (Exception ignored) {
                    // ignored: some managed Redis (or restricted users) may not permit CONFIG SET
                }
                return null;
            });
        } catch (Exception ignored) {
            // ignore enabling errors; notifications may already be enabled or disabled by server config
        }

        return container;
    }
}