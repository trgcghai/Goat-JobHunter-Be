package iuh.fit.goat.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TotalStatisticsResponse {
    private Map<String, Long> users;
    private long totalJobs;
    private long totalBlogs;
    private long totalApplications;
}