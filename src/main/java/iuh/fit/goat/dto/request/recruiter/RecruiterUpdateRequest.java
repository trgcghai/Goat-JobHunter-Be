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
    @NotNull(message = "Account ID is required")
    private Long accountId;

    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private LocalDate dob;
    private Gender gender;
    private String position;
    private String avatar;
}