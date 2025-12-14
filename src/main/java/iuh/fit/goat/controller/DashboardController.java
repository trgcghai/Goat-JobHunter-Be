package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.enumeration.Status;
import iuh.fit.goat.dto.response.dashboard.TopBlogResponse;
import iuh.fit.goat.dto.response.dashboard.TotalStatisticsResponse;
import iuh.fit.goat.entity.Application;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DashboardController {
//    private final DashboardService dashboardService;
//
//    @GetMapping("/dashboard/users")
//    public ResponseEntity<?> statisticsUser() {
//        Map<String, Long> result = this.dashboardService.handleStatisticsUser();
//        return ResponseEntity.status(HttpStatus.OK).body(result);
//    }
//
//    @GetMapping("/dashboard/jobs")
//    public ResponseEntity<?> statisticsJob(@Filter Specification<Job> spec) {
//        Map<String, Long> result = this.dashboardService.handleStatisticsJob(spec);
//        return ResponseEntity.status(HttpStatus.OK).body(result);
//    }
//
//    @GetMapping("/dashboard/applications")
//    public ResponseEntity<?> statisticsApplication(@Filter Specification<Application> spec) {
//        Map<Status, Long> result = this.dashboardService.handleStatisticsApplication(spec);
//        return ResponseEntity.status(HttpStatus.OK).body(result);
//    }
//
//    @GetMapping("/dashboard/applications-year")
//    public ResponseEntity<?> statisticsApplicationByYear(int year, @Filter Specification<Application> spec) {
//        Map<Integer, Long> result = this.dashboardService.handleStatisticsApplicationByYear(year, spec);
//        return ResponseEntity.status(HttpStatus.OK).body(result);
//    }
//
//    @GetMapping("/dashboard/total")
//    public ResponseEntity<TotalStatisticsResponse> getTotalStatistics() {
//        TotalStatisticsResponse result = this.dashboardService.handleTotalStatistics();
//        return ResponseEntity.status(HttpStatus.OK).body(result);
//    }
//
//    @GetMapping("/dashboard/applications/year")
//    public ResponseEntity<Map<Status, Map<Integer, Long>>> getApplicationsByYear(@RequestParam int year) {
//        Map<Status, Map<Integer, Long>> result = this.dashboardService.handleApplicationsByStatusAndMonth(year);
//        return ResponseEntity.status(HttpStatus.OK).body(result);
//    }
//
//    @GetMapping("/dashboard/blogs/top10")
//    public ResponseEntity<List<TopBlogResponse>> getTop10Blogs(@RequestParam int year, @RequestParam int month) {
//        List<TopBlogResponse> result = this.dashboardService.handleTop10BlogsByMonth(year, month);
//        return ResponseEntity.status(HttpStatus.OK).body(result);
//    }
}
