package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.ai.ChatRequest;

public interface AiService {
    String chatWithAi(ChatRequest request);

    String getTopJobsContext();

    String getTopApplicantsContext();

    String getTopRecruitersContext();

    String getRecentApplicationsContext();

    String getTopSkillsContext();

    String getRecentBlogsContext();

    String getAllCareersContext();

    String getSystemStatsContext();

    String getJobMarketOverview();
}
