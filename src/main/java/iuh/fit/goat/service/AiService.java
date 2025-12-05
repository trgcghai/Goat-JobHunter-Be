package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.ai.ChatRequest;

import java.util.List;

public interface AiService {
    String chatWithAi(ChatRequest request);

    String getTopJobsContext();

    String getTopApplicantsContext();

    String getTopRecruitersContext();

    String getRecentApplicationsContext();

    String getTopSkillsContext();

    String getRecentBlogsContext();

    String getAllCareersContext();

    String getConversationHistory(Long conversationId);

    String generateBlogDescription(String content);

    List<String> generateBlogTags(String content);
    String getSystemStatsContext();

    String getJobMarketOverview();
}
