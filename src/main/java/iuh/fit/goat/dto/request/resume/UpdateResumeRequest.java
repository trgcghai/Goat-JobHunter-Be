package iuh.fit.goat.dto.request.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateResumeRequest {
    @NotNull(message = "Resume ID is required")
    private Long resumeId;
    @NotBlank(message = "Title is required")
    private String title;
}
