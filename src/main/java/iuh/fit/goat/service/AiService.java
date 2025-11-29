package iuh.fit.goat.service;

import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Recruiter;

public interface AiService {
    String chatWithAi(String userMessageContent);

    String getAllJobsContext();

    String getAllApplicantsContext();

    String getAllRecruitersContext();

    String getAllApplicationsContext();

    String getJobsContextForRecruiter(Recruiter recruiter);

    String getApplicationsContextForRecruiter(Recruiter recruiter);

    String getApplicationsContextForApplicant(Applicant applicant);
}
