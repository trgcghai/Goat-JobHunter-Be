package iuh.fit.goat.dto.request.application;

import iuh.fit.goat.util.annotation.ValidApplicationRequest;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidApplicationRequest
public class ApplicationIdsRequest {
    @NotEmpty(message = "applicationIds is required")
    private List<Long> applicationIds;
    private LocalDate interviewDate;
    private String interviewType;
    private String location;
    private String note;
    private String reason;
}
