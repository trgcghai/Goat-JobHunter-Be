package iuh.fit.goat.service;

import iuh.fit.goat.common.Status;
import iuh.fit.goat.dto.response.dashboard.TopBlogResponse;
import iuh.fit.goat.dto.response.dashboard.TotalStatisticsResponse;
import iuh.fit.goat.entity.Application;
import iuh.fit.goat.entity.Job;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

public interface DashboardService {
    Map<String, Long> handleStatisticsUser();

    Map<String, Long> handleStatisticsJob(Specification<Job> spec);

    Map<Status, Long> handleStatisticsApplication(Specification<Application> spec);

    Map<Integer, Long> handleStatisticsApplicationByYear(int year, Specification<Application> spec);

    TotalStatisticsResponse handleTotalStatistics();

    Map<Status, Map<Integer, Long>> handleApplicationsByStatusAndMonth(int year);

    List<TopBlogResponse> handleTop10BlogsByMonth(int year, int month);
}
