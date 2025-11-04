package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.Status;
import iuh.fit.goat.entity.Application;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.repository.ApplicantRepository;
import iuh.fit.goat.repository.ApplicationRepository;
import iuh.fit.goat.repository.JobRepository;
import iuh.fit.goat.repository.RecruiterRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements iuh.fit.goat.service.DashboardService {
    private final ApplicantRepository applicantRepository;
    private final RecruiterRepository recruiterRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    public DashboardServiceImpl(ApplicantRepository applicantRepository, RecruiterRepository recruiterRepository,
                            JobRepository jobRepository, ApplicationRepository applicationRepository)
    {
        this.applicantRepository = applicantRepository;
        this.recruiterRepository = recruiterRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    public Map<String, Long> handleStatisticsUser(){
        long countApplicants = this.applicantRepository.count();
        long countRecruiters = this.recruiterRepository.count() - 1;

        return Map.of(
                "applicants", countApplicants,
                "recruiters", countRecruiters
        );
    }

    @Override
    public Map<String, Long> handleStatisticsJob(Specification<Job> spec){
        long totalJobs = this.jobRepository.count(spec);

        Specification<Job> activeJobSpec = spec.and((root, query, cb) ->
                cb.isTrue(root.get("active"))
        );
        long countActiveJob = this.jobRepository.count(activeJobSpec);

        return Map.of(
                "active", countActiveJob,
                "inactive", totalJobs - countActiveJob
        );
    }

    @Override
    public Map<Status, Long> handleStatisticsApplication(Specification<Application> spec){
        List<Application> applications = this.applicationRepository.findAll(spec);

        Map<Status, Long> result = applications.stream()
                .collect(Collectors.groupingBy(
                        Application::getStatus,
                        () -> new EnumMap<>(Status.class),
                        Collectors.counting()
                ));
        for (Status status : Status.values()) {
            result.putIfAbsent(status, 0L);
        }

        return result;
    }

    @Override
    public Map<Integer, Long> handleStatisticsApplicationByYear(int year, Specification<Application> spec){
        List<Application> applications = this.applicationRepository.findAll(spec);

        Map<Integer, Long> groupedApplications = applications.stream()
                .filter(app -> app.getCreatedAt().atZone(ZoneId.systemDefault()).getYear() == year)
                .collect(Collectors.groupingBy(
                        app -> app.getCreatedAt().atZone(ZoneId.systemDefault()).getMonthValue(),
                        Collectors.counting()
                ));

        Map<Integer, Long> fullMonths = new LinkedHashMap<>();
        for (int month = 1; month <= 12; month++) {
            fullMonths.put(month, groupedApplications.getOrDefault(month, 0L));
        }

        return fullMonths;
    }
}
