package iuh.fit.goat.config.component;

import iuh.fit.goat.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
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

        System.out.println("AI cache preloaded!");
    }

}
