package iuh.fit.goat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.goat.dto.request.recruiter.RecruiterUpdateRequest;
import iuh.fit.goat.dto.response.recruiter.RecruiterResponse;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.enumeration.Gender;
import iuh.fit.goat.enumeration.Visibility;
import iuh.fit.goat.repository.AddressRepository;
import iuh.fit.goat.repository.RecruiterRepository;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecruiterServiceImplTest {

    @Mock private StorageService storageService;
    @Mock private ProfileRealtimeService profileRealtimeService;
    @Mock private RecruiterRepository recruiterRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private ObjectMapper mapper;

    @InjectMocks
    private RecruiterServiceImpl recruiterService;

    private Recruiter recruiter;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setRoleId(3L);
        role.setName("RECRUITER");

        recruiter = new Recruiter();
        recruiter.setAccountId(202L);
        recruiter.setEmail("recruiter@example.com");
        recruiter.setUsername("recruiter1");
        recruiter.setFullName("Recruiter One");
        recruiter.setPassword("hash");
        recruiter.setRole(role);
        recruiter.setEnabled(true);
        recruiter.setGender(Gender.MALE);
        recruiter.setVisibility(Visibility.PUBLIC);
        recruiter.setPosition("HR");
    }

    @Test
    void handleUpdateRecruiter_shouldUpdateBasicFieldsAndEmitEvent() throws Exception {
        RecruiterUpdateRequest request = new RecruiterUpdateRequest();
        request.setAccountId(202L);
        request.setUsername("updated-recruiter");
        request.setFullName("Updated Recruiter");
        request.setEmail("updated-recruiter@example.com");
        request.setPhone("0901111111");
        request.setDob(LocalDate.of(1995, 5, 20));
        request.setGender(Gender.FEMALE);
        request.setHeadline("Headline");
        request.setBio("Bio");
        request.setPosition("Talent Acquisition");

        when(recruiterRepository.findById(202L)).thenReturn(Optional.of(recruiter));
        when(recruiterRepository.save(any(Recruiter.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recruiterRepository.findById(202L)).thenReturn(Optional.of(recruiter));

        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUserEmail).thenReturn("recruiter@example.com");

            Recruiter result = recruiterService.handleUpdateRecruiter(request);

            assertThat(result.getUsername()).isEqualTo("updated-recruiter");
            assertThat(result.getPosition()).isEqualTo("Talent Acquisition");
            assertThat(result.getPhone()).isEqualTo("0901111111");
            verify(profileRealtimeService).emitUserProfileUpdated(eq("recruiter@example.com"), eq("RECRUITER"), any(RecruiterResponse.class));
        }
    }

    @Test
    void convertToRecruiterResponse_shouldMapCoreFields() {
        RecruiterResponse response = recruiterService.convertToRecruiterResponse(recruiter);

        assertThat(response.getAccountId()).isEqualTo(202L);
        assertThat(response.getEmail()).isEqualTo("recruiter@example.com");
        assertThat(response.getPosition()).isEqualTo("HR");
        assertThat(response.getVisibility()).isEqualTo(Visibility.PUBLIC);
    }
}