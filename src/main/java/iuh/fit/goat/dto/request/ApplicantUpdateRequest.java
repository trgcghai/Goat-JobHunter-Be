package iuh.fit.goat.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.common.Education;
import iuh.fit.goat.common.Gender;
import iuh.fit.goat.common.Level;
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
    private Education education;
    private Level level;
    private String avatar;
}