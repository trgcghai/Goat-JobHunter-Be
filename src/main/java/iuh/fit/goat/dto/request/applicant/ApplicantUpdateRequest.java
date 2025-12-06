package iuh.fit.goat.dto.request.applicant;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.enumeration.Education;
import iuh.fit.goat.enumeration.Gender;
import iuh.fit.goat.enumeration.Level;
import iuh.fit.goat.entity.embeddable.Contact;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicantUpdateRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    private String username;
    private String fullName;
    private Contact contact;
    private String address;
    private LocalDate dob;
    private Gender gender;
    private Education education;
    private Level level;
    private String avatar;
    private boolean availableStatus;
}