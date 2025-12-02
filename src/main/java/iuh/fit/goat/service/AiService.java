package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.ai.ChatRequest;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Recruiter;

public interface AiService {
    String chatWithAi(ChatRequest request);

    String getAllJobsContext();

    String getAllApplicantsContext();

    String getAllRecruitersContext();

    String getAllApplicationsContext();

    String getJobsContextForRecruiter(Recruiter recruiter);

    String getApplicationsContextForRecruiter(Recruiter recruiter);

    String getApplicationsContextForApplicant(Applicant applicant);

    String getConversationHistory(Long conversationId);
}
