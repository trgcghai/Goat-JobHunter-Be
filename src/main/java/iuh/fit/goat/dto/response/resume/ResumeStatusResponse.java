package iuh.fit.goat.dto.response.resume;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeStatusResponse {
    private Long resumeId;
    private boolean result;
}