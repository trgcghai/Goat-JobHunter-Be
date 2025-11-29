package iuh.fit.goat.dto.request.subscriber;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberUpdateDto {
    @NotNull(message = "Subscriber ID is required")
    private Long subscriberId;

    private String name;

    @Email(message = "Email is not valid")
    private String email;

    private List<Long> skillIds;
}