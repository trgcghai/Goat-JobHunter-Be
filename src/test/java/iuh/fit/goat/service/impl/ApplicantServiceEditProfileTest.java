package iuh.fit.goat.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import iuh.fit.goat.dto.request.applicant.ApplicantUpdateRequest;
import iuh.fit.goat.dto.response.StorageResponse;
import iuh.fit.goat.entity.Address;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.enumeration.Gender;
import iuh.fit.goat.exception.InvalidException;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicantServiceEditProfileTest {

    @Mock private StorageService storageService;
    @Mock private ProfileRealtimeService profileRealtimeService;
    @Mock private ApplicantRepository applicantRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper mapper;

    @InjectMocks
    private ApplicantServiceImpl applicantService;

    private Applicant existingApplicant;

    @BeforeEach
    void setUp() {
        existingApplicant = new Applicant();
        existingApplicant.setAccountId(101L);
        existingApplicant.setUsername("olduser");
        existingApplicant.setFullName("Old Name");
        existingApplicant.setEmail("old@example.com");
        existingApplicant.setPhone("0123456789");
        existingApplicant.setDob(LocalDate.of(1990,1,1));
        existingApplicant.setGender(Gender.MALE);
        existingApplicant.setAvatar("https://s3.amazonaws.com/bucket/old-avatar.jpg");
        existingApplicant.setCoverPhoto("https://s3.amazonaws.com/bucket/old-cover.jpg");

        Address a1 = new Address();
        a1.setAddressId(11L);
        a1.setProvince("Hanoi");
        a1.setFullAddress("Old Address 1");

        Address a2 = new Address();
        a2.setAddressId(12L);
        a2.setProvince("HCM");
        a2.setFullAddress("Old Address 2");

        existingApplicant.setAddresses(new ArrayList<>(List.of(a1, a2)));
    }

    @Test
    void updateReturnsNullWhenApplicantMissing() throws Exception {
        ApplicantUpdateRequest req = new ApplicantUpdateRequest();
        req.setAccountId(999L);
        when(applicantRepository.findById(999L)).thenReturn(Optional.empty());

        Applicant res = applicantService.handleUpdateApplicant(req);

        assertThat(res).isNull();
    }

    @Test
    void updateBasicFields_appliesAllProvided() throws Exception {
        ApplicantUpdateRequest req = new ApplicantUpdateRequest();
        req.setAccountId(101L);
        req.setUsername("newuser");
        req.setFullName("New Name");
        req.setEmail("new@example.com");
        req.setPhone("0987654321");
        req.setDob(LocalDate.of(1995,5,5));
        req.setAvailableStatus(true);
        req.setHeadline("Headline");
        req.setBio("Bio");

        when(applicantRepository.findById(101L)).thenReturn(Optional.of(existingApplicant));
        when(applicantRepository.save(any(Applicant.class))).thenAnswer(i->i.getArgument(0));

        Applicant out = applicantService.handleUpdateApplicant(req);

        assertThat(out.getUsername()).isEqualTo("newuser");
        assertThat(out.getFullName()).isEqualTo("New Name");
        assertThat(out.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void avatarUpload_callsStorageAndDeletesOldKey() throws Exception {
        ApplicantUpdateRequest req = new ApplicantUpdateRequest();
        req.setAccountId(101L);

        when(applicantRepository.findById(101L)).thenReturn(Optional.of(existingApplicant));
        when(applicantRepository.save(any(Applicant.class))).thenAnswer(i->i.getArgument(0));

        when(storageService.handleUploadFile(any(), eq("avatars")))
                .thenReturn(CompletableFuture.completedFuture(new StorageResponse("p","https://s3.amazonaws.com/bucket/new.jpg")));

        applicantService.handleUpdateApplicant(req);

        verify(storageService, atLeastOnce()).handleUploadFile(any(), eq("avatars"));
    }

    @Test
    void coverPhotoUpload_callsStorageAndDeletesOldKey() throws Exception {
        ApplicantUpdateRequest req = new ApplicantUpdateRequest();
        req.setAccountId(101L);

        when(applicantRepository.findById(101L)).thenReturn(Optional.of(existingApplicant));
        when(applicantRepository.save(any(Applicant.class))).thenAnswer(i->i.getArgument(0));

        when(storageService.handleUploadFile(any(), eq("user-cover-photos")))
                .thenReturn(CompletableFuture.completedFuture(new StorageResponse("p","https://s3.amazonaws.com/bucket/new-cover.jpg")));

        applicantService.handleUpdateApplicant(req);

        verify(storageService, atLeastOnce()).handleUploadFile(any(), eq("user-cover-photos"));
    }

    @Test
    void addressesInvalidJson_throwsInvalidException() throws Exception {
        ApplicantUpdateRequest req = new ApplicantUpdateRequest();
        req.setAccountId(101L);
        req.setAddresses("badjson");

        when(applicantRepository.findById(101L)).thenReturn(Optional.of(existingApplicant));
        when(mapper.readValue(eq("badjson"), any(TypeReference.class))).thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "err"));

        assertThatThrownBy(() -> applicantService.handleUpdateApplicant(req)).isInstanceOf(InvalidException.class);
    }

    @Test
    void addressesDeleteMissing_removesOldOnes() throws Exception {
        Address reqAddr = new Address(); reqAddr.setAddressId(11L); reqAddr.setProvince("Hanoi"); reqAddr.setFullAddress("Old Address 1");
        when(applicantRepository.findById(101L)).thenReturn(Optional.of(existingApplicant));
        when(mapper.readValue(eq("[]"), any(TypeReference.class))).thenReturn(List.of(reqAddr));
        when(applicantRepository.save(any(Applicant.class))).thenAnswer(i->i.getArgument(0));

        applicantService.handleUpdateApplicant(new ApplicantUpdateRequest(){ { setAccountId(101L); setAddresses("[]"); } });

        verify(addressRepository, times(1)).delete(any(Address.class));
    }

    @Test
    void addressesUpdateAndCreate_behavesCorrectly() throws Exception {
        Address upd = new Address(); upd.setAddressId(11L); upd.setProvince("P2"); upd.setFullAddress("FA");
        Address create = new Address(); create.setAddressId(0L); create.setProvince("NP"); create.setFullAddress("New");
        when(applicantRepository.findById(101L)).thenReturn(Optional.of(existingApplicant));
        when(mapper.readValue(eq("[]"), any(TypeReference.class))).thenReturn(List.of(upd, create));
        when(applicantRepository.save(any(Applicant.class))).thenAnswer(i->i.getArgument(0));

        Applicant out = applicantService.handleUpdateApplicant(new ApplicantUpdateRequest(){ { setAccountId(101L); setAddresses("[]"); } });

        assertThat(out.getAddresses()).anyMatch(a -> "NP".equals(a.getProvince()));
    }

    @Test
    void emitsRealtimeEvent_whenSecurityEmailPresent() throws Exception {
        when(applicantRepository.findById(101L)).thenReturn(Optional.of(existingApplicant));
        when(applicantRepository.save(any(Applicant.class))).thenAnswer(i->i.getArgument(0));

        try (MockedStatic<SecurityUtil> s = mockStatic(SecurityUtil.class)) {
            s.when(SecurityUtil::getCurrentUserEmail).thenReturn("me@x.com");

            applicantService.handleUpdateApplicant(new ApplicantUpdateRequest(){ { setAccountId(101L); } });

            verify(profileRealtimeService).emitUserProfileUpdated(eq("me@x.com"), eq("APPLICANT"), any());
        }
    }

    @Test
    void doesNotEmit_whenSecurityEmailBlank() throws Exception {
        when(applicantRepository.findById(101L)).thenReturn(Optional.of(existingApplicant));
        when(applicantRepository.save(any(Applicant.class))).thenAnswer(i->i.getArgument(0));

        try (MockedStatic<SecurityUtil> s = mockStatic(SecurityUtil.class)) {
            s.when(SecurityUtil::getCurrentUserEmail).thenReturn("");

            applicantService.handleUpdateApplicant(new ApplicantUpdateRequest(){ { setAccountId(101L); } });

            verify(profileRealtimeService, never()).emitUserProfileUpdated(anyString(), anyString(), any());
        }
    }

    @Test
    void basicUtilUpload_throwsWhenNoUrlFromStorage() {
        when(storageService.handleUploadFile(any(), eq("avatars")))
                .thenReturn(CompletableFuture.completedFuture(new StorageResponse("p", null)));

        assertThatThrownBy(() -> iuh.fit.goat.util.BasicUtil.uploadImage(null, "avatars", storageService)).isInstanceOf(InvalidException.class);
    }
}
