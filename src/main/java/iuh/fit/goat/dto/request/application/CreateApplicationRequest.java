package iuh.fit.goat.dto.request.application;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApplicationRequest {
    @Email(message = "Email should be valid")
    private String email; // email này là email để thông báo tới applicant, không phải email của applicant đăng nhập

    @NotBlank(message = "Cover letter is required")
    @Size(max = 3000, message = "Cover letter must not exceed 3000 characters")
    private String coverLetter;

    @NotNull(message = "Job ID is required")
    @Positive(message = "Job ID must be positive")
    private Long jobId;

    @NotNull(message = "Resume ID is required")
    @Positive(message = "Resume ID must be positive")
    private Long resumeId;
}
