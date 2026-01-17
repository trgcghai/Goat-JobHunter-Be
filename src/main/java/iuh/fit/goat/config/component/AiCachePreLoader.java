package iuh.fit.goat.config.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class AiCachePreLoader implements ApplicationRunner {

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        log.info("AI cache preloaded!");
    }

}
