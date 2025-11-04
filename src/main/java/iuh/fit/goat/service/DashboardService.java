package iuh.fit.goat.service;

import iuh.fit.goat.common.Status;
import iuh.fit.goat.entity.Application;
import iuh.fit.goat.entity.Job;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

public interface DashboardService {
    Map<String, Long> handleStatisticsUser();

    Map<String, Long> handleStatisticsJob(Specification<Job> spec);

    Map<Status, Long> handleStatisticsApplication(Specification<Application> spec);

    Map<Integer, Long> handleStatisticsApplicationByYear(int year, Specification<Application> spec);
}
