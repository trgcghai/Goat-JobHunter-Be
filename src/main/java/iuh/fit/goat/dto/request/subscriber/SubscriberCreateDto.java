package iuh.fit.goat.dto.request.subscriber;

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
    @Size(min = 0)
    private List<Long> skillIds;
}
