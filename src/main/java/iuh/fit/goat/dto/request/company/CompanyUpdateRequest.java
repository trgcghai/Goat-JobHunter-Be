package iuh.fit.goat.dto.request.company;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.entity.Address;
import iuh.fit.goat.enumeration.CompanySize;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompanyUpdateRequest {
    @NotNull(message = "Account ID is required")
    private Long accountId;
    private String username;
    private List<Address> addresses;
    private String name;
    private String description;
    private String logo;
    private String coverPhoto;
    private String website;
    private String phone;
    private CompanySize size;
    private String country;
    private String industry;
    private String workingDays;
    private String overtimePolicy;
}
