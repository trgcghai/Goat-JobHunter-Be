package iuh.fit.goat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.goat.dto.request.applicant.ApplicantUpdateRequest;
import iuh.fit.goat.dto.response.applicant.ApplicantResponse;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.enumeration.Gender;
import iuh.fit.goat.enumeration.Visibility;
import iuh.fit.goat.repository.AddressRepository;
import iuh.fit.goat.repository.ApplicantRepository;
import iuh.fit.goat.service.ProfileRealtimeService;
import iuh.fit.goat.service.StorageService;
import iuh.fit.goat.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicantServiceImplTest {

    @Mock private StorageService storageService;
    @Mock private ProfileRealtimeService profileRealtimeService;
    @Mock private ApplicantRepository applicantRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private ObjectMapper mapper;

    @InjectMocks
    private ApplicantServiceImpl applicantService;

    private Applicant applicant;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setRoleId(2L);
        role.setName("APPLICANT");

        applicant = new Applicant();
        applicant.setAccountId(101L);
        applicant.setEmail("applicant@example.com");
        applicant.setUsername("applicant1");
        applicant.setFullName("Applicant One");
        applicant.setPassword("hash");
        applicant.setRole(role);
        applicant.setEnabled(true);
        applicant.setGender(Gender.MALE);
        applicant.setVisibility(Visibility.PUBLIC);
        applicant.setAvailableStatus(false);
    }

    @Test
    void handleUpdateApplicant_shouldUpdateBasicFieldsAndEmitEvent() throws Exception {
        ApplicantUpdateRequest request = new ApplicantUpdateRequest();
        request.setAccountId(101L);
        request.setUsername("updated-name");
        request.setFullName("Updated Applicant");
        request.setEmail("updated@example.com");
        request.setPhone("0900000000");
        request.setDob(LocalDate.of(2000, 1, 1));
        request.setGender(Gender.FEMALE);
        request.setAvailableStatus(true);
        request.setHeadline("New headline");
        request.setBio("New bio");

        when(applicantRepository.findById(101L)).thenReturn(Optional.of(applicant));
        when(applicantRepository.save(any(Applicant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicantRepository.findById(101L)).thenReturn(Optional.of(applicant));

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserEmail).thenReturn("applicant@example.com");

            Applicant result = applicantService.handleUpdateApplicant(request);

            assertThat(result.getUsername()).isEqualTo("updated-name");
            assertThat(result.getFullName()).isEqualTo("Updated Applicant");
            assertThat(result.getEmail()).isEqualTo("updated@example.com");
            assertThat(result.getPhone()).isEqualTo("0900000000");
            assertThat(result.getDob()).isEqualTo(LocalDate.of(2000, 1, 1));
            assertThat(result.getGender()).isEqualTo(Gender.FEMALE);
            assertThat(result.isAvailableStatus()).isTrue();
            verify(profileRealtimeService).emitUserProfileUpdated(eq("applicant@example.com"), eq("APPLICANT"), any(ApplicantResponse.class));
        }
    }

    @Test
    void convertToApplicantResponse_shouldMapCoreFields() {
        ApplicantResponse response = applicantService.convertToApplicantResponse(applicant);

        assertThat(response.getAccountId()).isEqualTo(101L);
        assertThat(response.getEmail()).isEqualTo("applicant@example.com");
        assertThat(response.getFullName()).isEqualTo("Applicant One");
        assertThat(response.isAvailableStatus()).isFalse();
        assertThat(response.getVisibility()).isEqualTo(Visibility.PUBLIC);
    }
}