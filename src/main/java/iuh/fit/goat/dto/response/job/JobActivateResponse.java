package iuh.fit.goat.dto.response.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobActivateResponse {
    private Long jobId;
    private boolean active;
    private String status; // "success" or "fail"
}