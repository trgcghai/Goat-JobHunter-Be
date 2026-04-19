package iuh.fit.goat.dto.request.poll;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClosePollRequest {
    @NotBlank(message = "Poll ID is required")
    private String pollId;
}

