package iuh.fit.goat.dto.request.job;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobIdsRequest {
    @NotEmpty(message = "jobIds must not be empty")
    private List<@NotNull(message = "jobId must not be null") Long> jobIds;
}
