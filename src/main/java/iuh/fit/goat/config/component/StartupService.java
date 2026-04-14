package iuh.fit.goat.config.component;

import iuh.fit.goat.service.RedisService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupService {

    private final RedisService redisService;

    @PostConstruct
    public void clearSessionsOnStartup() {
        log.warn("🔥 Server restarted — clearing all login sessions");
        this.redisService.deleteByPattern("account:*");
    }
}
