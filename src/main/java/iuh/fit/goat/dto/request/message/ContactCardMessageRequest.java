package iuh.fit.goat.dto.request.message;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactCardMessageRequest {

    @NotNull(message = "User IDs are required")
    @NotEmpty(message = "At least one user ID is required")
    private List<Long> userIds;
}
