package iuh.fit.goat.dto.request.subscriber;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberCreateDto {
    @NotBlank(message = "Name is not empty")
    private String name;

    @NotBlank(message = "Email is not empty")
    @Email(message = "Email is not valid")
    private String email;

    @Size(min = 0)
    private List<Long> skillIds;
}
