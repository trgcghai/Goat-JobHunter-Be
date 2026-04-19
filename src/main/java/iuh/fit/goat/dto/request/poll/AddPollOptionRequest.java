package iuh.fit.goat.dto.request.poll;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddPollOptionRequest {
    @NotBlank(message = "Poll ID is required")
    private String pollId;
    @NotBlank(message = "Option text is required")
    @Size(min = 1, max = 200, message = "Option text must be between 1 and 200 characters")
    private String text;
}

