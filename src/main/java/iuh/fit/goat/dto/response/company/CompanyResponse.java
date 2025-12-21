package iuh.fit.goat.dto.response.company;

import iuh.fit.goat.entity.Address;
import iuh.fit.goat.enumeration.CompanySize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {
    private long accountId;
    private String email;
    private String name;
    private String description;
    private String logo;
    private String coverPhoto;
    private String website;
    private List<CompanyAddress> addresses;
    private String phone;
    private CompanySize size;
    private boolean verified;
    private Instant createdAt;
    private Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyAddress {
        private long addressId;
        private String province;
        private String fullAddress;
    }
}
