package iuh.fit.goat.dto.response.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationCountResponse {
    private Long jobId;
    private Long applications;
}
