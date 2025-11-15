package iuh.fit.goat.service;

import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Recruiter;
import org.springframework.transaction.annotation.Transactional;

public interface AiService {
    @Transactional(readOnly = true)
    String chatWithAi(String userMessageContent);

    @Transactional(readOnly = true)
    String getAllJobsContext();

    @Transactional(readOnly = true)
    String getAllApplicantsContext();

    @Transactional(readOnly = true)
    String getAllRecruitersContext();

    @Transactional(readOnly = true)
    String getAllApplicationsContext();

    @Transactional(readOnly = true)
    String getJobsContextForRecruiter(Recruiter recruiter);

    @Transactional(readOnly = true)
    String getApplicationsContextForRecruiter(Recruiter recruiter);

    @Transactional(readOnly = true)
    String getApplicationsContextForApplicant(Applicant applicant);
}
