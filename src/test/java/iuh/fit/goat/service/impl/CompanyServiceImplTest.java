package iuh.fit.goat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.goat.dto.request.company.CompanyUpdateRequest;
import iuh.fit.goat.dto.response.company.CompanyResponse;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.enumeration.CompanySize;
import iuh.fit.goat.enumeration.Visibility;
import iuh.fit.goat.repository.AddressRepository;
import iuh.fit.goat.repository.CompanyRepository;
import iuh.fit.goat.service.ProfileRealtimeService;
import iuh.fit.goat.service.RoleService;
import iuh.fit.goat.service.StorageService;
import iuh.fit.goat.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock private RoleService roleService;
    @Mock private StorageService storageService;
    @Mock private ProfileRealtimeService profileRealtimeService;
    @Mock private CompanyRepository companyRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private ObjectMapper mapper;

    @InjectMocks
    private CompanyServiceImpl companyService;

    private Company company;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setRoleId(4L);
        role.setName("COMPANY");

        company = new Company();
        company.setAccountId(303L);
        company.setEmail("company@example.com");
        company.setUsername("company1");
        company.setName("Goat Corp");
        company.setPassword("hash");
        company.setRole(role);
        company.setEnabled(true);
        company.setVerified(true);
        company.setVisibility(Visibility.PUBLIC);
        company.setSize(CompanySize.MEDIUM);
    }

    @Test
    void handleUpdateCompany_shouldUpdateBasicFieldsAndEmitEvent() throws Exception {
        CompanyUpdateRequest request = new CompanyUpdateRequest();
        request.setAccountId(303L);
        request.setUsername("updated-company");
        request.setName("Updated Goat Corp");
        request.setDescription("New desc");
        request.setWebsite("https://example.com");
        request.setPhone("0902222222");
        request.setCountry("VN");
        request.setIndustry("Tech");
        request.setWorkingDays("Mon-Fri");
        request.setOvertimePolicy("Paid");
        request.setSize(CompanySize.LARGE);

        when(companyRepository.findByAccountIdAndDeletedAtIsNull(303L)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyRepository.findById(303L)).thenReturn(Optional.of(company));

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserEmail).thenReturn("company@example.com");

            Company result = companyService.handleUpdateCompany(request);

            assertThat(result.getName()).isEqualTo("Updated Goat Corp");
            assertThat(result.getWebsite()).isEqualTo("https://example.com");
            assertThat(result.getSize()).isEqualTo(CompanySize.LARGE);
            verify(profileRealtimeService).emitUserProfileUpdated(eq("company@example.com"), eq("COMPANY"), any(CompanyResponse.class));
        }
    }

    @Test
    void convertToCompanyResponse_shouldMapCoreFields() {
        CompanyResponse response = companyService.convertToCompanyResponse(company);

        assertThat(response.getAccountId()).isEqualTo(303L);
        assertThat(response.getEmail()).isEqualTo("company@example.com");
        assertThat(response.getName()).isEqualTo("Goat Corp");
        assertThat(response.getSize()).isEqualTo(CompanySize.MEDIUM);
    }
}