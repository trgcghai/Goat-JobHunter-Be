package iuh.fit.goat.dto.response.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusResponse {
    private Long applicationId;
    private String status;
}
