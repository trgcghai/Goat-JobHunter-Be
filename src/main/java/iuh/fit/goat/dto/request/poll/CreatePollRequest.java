package iuh.fit.goat.dto.request.poll;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePollRequest {
    @NotBlank(message = "Question is required")
    @Size(min = 1, max = 200, message = "Question must be between 1 and 200 characters")
    private String question;
    @NotEmpty(message = "At least 2 options are required")
    @Size(min = 2, max = 10, message = "Poll must have between 2 and 10 options")
    private List<String> options;
    @NotNull(message = "multipleChoice field is required")
    private Boolean multipleChoice;
    @NotNull(message = "allowAddOption field is required")
    private Boolean allowAddOption;
    @NotNull(message = "pinned field is required")
    private Boolean pinned;
    private Instant expiresAt;
}

