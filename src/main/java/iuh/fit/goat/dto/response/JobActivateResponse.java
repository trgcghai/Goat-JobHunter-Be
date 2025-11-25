package iuh.fit.goat.dto.response;

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
