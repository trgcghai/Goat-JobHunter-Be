package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.Status;
import iuh.fit.goat.dto.response.dashboard.TopBlogResponse;
import iuh.fit.goat.dto.response.dashboard.TotalStatisticsResponse;
import iuh.fit.goat.entity.Application;
import iuh.fit.goat.entity.Blog;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final ApplicantRepository applicantRepository;
    private final RecruiterRepository recruiterRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final BlogRepository blogRepository;

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


    @Override
    public TotalStatisticsResponse handleTotalStatistics() {
        long countApplicants = this.applicantRepository.count();
        long countRecruiters = this.recruiterRepository.count() - 1;
        long countAdmins = this.userRepository.count() - countApplicants - countRecruiters;

        Map<String, Long> users = Map.of(
                "applicants", countApplicants,
                "recruiters", countRecruiters,
                "admins", countAdmins
        );

        long totalJobs = this.jobRepository.count();
        long totalBlogs = this.blogRepository.count();
        long totalApplications = this.applicationRepository.count();

        return new TotalStatisticsResponse(users, totalJobs, totalBlogs, totalApplications);
    }

    @Override
    public Map<Status, Map<Integer, Long>> handleApplicationsByStatusAndMonth(int year) {
        List<Application> applications = this.applicationRepository.findAll().stream()
                .filter(app -> app.getCreatedAt().atZone(ZoneId.systemDefault()).getYear() == year)
                .toList();

        Map<Status, Map<Integer, Long>> result = new EnumMap<>(Status.class);

        for (Status status : Status.values()) {
            Map<Integer, Long> monthlyData = applications.stream()
                    .filter(app -> app.getStatus() == status)
                    .collect(Collectors.groupingBy(
                            app -> app.getCreatedAt().atZone(ZoneId.systemDefault()).getMonthValue(),
                            Collectors.counting()
                    ));

            Map<Integer, Long> fullMonths = new LinkedHashMap<>();
            for (int month = 1; month <= 12; month++) {
                fullMonths.put(month, monthlyData.getOrDefault(month, 0L));
            }
            result.put(status, fullMonths);
        }

        return result;
    }

    @Override
    public List<TopBlogResponse> handleTop10BlogsByMonth(int year, int month) {
        List<Blog> blogs = this.blogRepository.findAll().stream()
                .filter(blog -> {
                    int blogYear = blog.getCreatedAt().atZone(ZoneId.systemDefault()).getYear();
                    int blogMonth = blog.getCreatedAt().atZone(ZoneId.systemDefault()).getMonthValue();
                    return blogYear == year && blogMonth == month;
                })
                .sorted((b1, b2) -> {
                    long score1 = b1.getActivity().getTotalReads() +
                            b1.getActivity().getTotalLikes() +
                            b1.getActivity().getTotalComments();
                    long score2 = b2.getActivity().getTotalReads() +
                            b2.getActivity().getTotalLikes() +
                            b2.getActivity().getTotalComments();
                    return Long.compare(score2, score1);
                })
                .limit(10)
                .toList();

        return blogs.stream()
                .map(blog -> new TopBlogResponse(
                        blog.getBlogId(),
                        blog.getTitle(),
                        blog.getActivity().getTotalLikes(),
                        blog.getActivity().getTotalComments(),
                        blog.getActivity().getTotalReads()
                ))
                .toList();
    }
}
