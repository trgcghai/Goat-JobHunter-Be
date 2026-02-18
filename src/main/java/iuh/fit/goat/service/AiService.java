package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.ai.ChatRequest;
import iuh.fit.goat.dto.response.resume.ResumeEvaluationResponse;
import iuh.fit.goat.exception.InvalidException;

import java.util.List;

public interface AiService {
    String chatWithAi(ChatRequest request);

    String getTopJobsContext();

    String getTopApplicantsContext();

    String getTopCompaniesContext();

    String getRecentApplicationsContext();

    String getTopSkillsContext();

    String getRecentBlogsContext();

    String getAllCareersContext();

    String getSystemStatsContext();

    String getJobMarketOverview();

    List<String> generateBlogTags(String content);

    ResumeEvaluationResponse evaluateResume(String resumeUrl) throws InvalidException;
}
