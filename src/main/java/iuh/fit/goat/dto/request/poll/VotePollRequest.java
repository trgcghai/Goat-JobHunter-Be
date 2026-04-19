package iuh.fit.goat.dto.request.poll;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VotePollRequest {
    @NotNull(message = "Poll ID is required")
    private String pollId;
    @NotNull(message = "Option IDs are required")
    private List<String> optionIds;
}

