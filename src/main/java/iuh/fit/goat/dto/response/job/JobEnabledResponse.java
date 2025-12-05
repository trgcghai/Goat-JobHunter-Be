package iuh.fit.goat.dto.response.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobEnabledResponse {
    private Long jobId;
    private boolean enabled;
}
