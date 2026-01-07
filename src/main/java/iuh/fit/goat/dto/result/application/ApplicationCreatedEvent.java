package iuh.fit.goat.dto.result.application;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApplicationCreatedEvent {
    private Long applicationId;
    private String applicantEmail;
    private String applicantName;
    private String companyEmail;
    private String companyName;
    private String jobTitle;
}
