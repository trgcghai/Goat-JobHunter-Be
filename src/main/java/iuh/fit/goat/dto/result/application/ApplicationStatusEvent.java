package iuh.fit.goat.dto.result.application;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApplicationStatusEvent {
    private String jobTitle;
    private String companyName;
}
