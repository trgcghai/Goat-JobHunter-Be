package iuh.fit.goat.config.component;

import iuh.fit.goat.service.AiService;
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
    private final AiService aiService;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) throws Exception {
//        this.aiService.getTopJobsContext();
//        this.aiService.getTopApplicantsContext();
//        this.aiService.getTopRecruitersContext();
//        this.aiService.getRecentApplicationsContext();
//        this.aiService.getTopSkillsContext();
//        this.aiService.getRecentBlogsContext();
//        this.aiService.getAllCareersContext();
//        this.aiService.getSystemStatsContext();
//        this.aiService.getJobMarketOverview();

        log.info("AI cache preloaded!");
    }

}
