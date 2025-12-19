package iuh.fit.goat.dto.response.company;

import iuh.fit.goat.enumeration.CompanySize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {
    private String email;
    private String name;
    private String description;
    private String logo;
    private String coverPhoto;
    private String website;
    private String address;
    private String phone;
    private CompanySize size;
    private boolean verified;
    private Instant createdAt;
    private Instant updatedAt;
}
