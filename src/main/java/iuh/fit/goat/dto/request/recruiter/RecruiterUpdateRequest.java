package iuh.fit.goat.dto.request.recruiter;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.enumeration.Gender;
import iuh.fit.goat.entity.embeddable.Contact;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecruiterUpdateRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    private String username;
    private String fullName;
    private Contact contact;
    private String address;
    private LocalDate dob;
    private Gender gender;
    private String description;
    private String website;
    private String avatar;
}