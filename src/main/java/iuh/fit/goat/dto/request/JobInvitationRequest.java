package iuh.fit.goat.dto.request;

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
public class JobInvitationRequest {
    @NotEmpty(message = "applicantIds must not be empty")
    private List<@NotNull(message = "applicantId must not be null") Long> applicantIds;
    @NotNull(message = "jobId must not be null")
    private Long jobId;
}
