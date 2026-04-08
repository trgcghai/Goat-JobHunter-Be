package iuh.fit.goat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.goat.dto.request.applicant.ApplicantUpdateRequest;
import iuh.fit.goat.dto.response.applicant.ApplicantResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.user.UserResponse;
import iuh.fit.goat.enumeration.Gender;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.ApplicantService;
import iuh.fit.goat.service.ProfileRealtimeService;
import iuh.fit.goat.service.RoleService;
import iuh.fit.goat.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.util.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicantServiceImpl implements ApplicantService {
    private final StorageService storageService;
    private final ProfileRealtimeService profileRealtimeService;

    private final ApplicantRepository applicantRepository;
    private final AddressRepository addressRepository;

    private final ObjectMapper mapper;
    private final String applicantRole = "APPLICANT";

    @Override
    public Applicant handleCreateApplicant(Applicant applicant) throws InvalidException {
        applicant.setEnabled(false);
        if (applicant.getAvatar() == null) {
            try {
                MultipartFile multipartFile = BasicUtil.convertToMultipartFile(
                        applicant.getGender() == Gender.MALE ? FileUploadUtil.AVATAR_MALE : FileUploadUtil.AVATAR_FEMALE
                );
                String avatarUrl = BasicUtil.uploadImage(multipartFile, "avatars", this.storageService);
                applicant.setAvatar(avatarUrl);
            } catch (IOException | InvalidException e) {
                throw new InvalidException("Failed to set default avatar");
            }
        }

        return this.applicantRepository.save(applicant);
    }

    @Override
    public Applicant handleUpdateApplicant(ApplicantUpdateRequest updateRequest) throws InvalidException {
        Applicant currentApplicant = this.handleGetApplicantById(updateRequest.getAccountId());
        if (currentApplicant == null) return null;

        if (updateRequest.getUsername() != null) currentApplicant.setUsername(updateRequest.getUsername());
        if (updateRequest.getFullName() != null) currentApplicant.setFullName(updateRequest.getFullName());
        if (updateRequest.getEmail() != null) currentApplicant.setEmail(updateRequest.getEmail());
        if (updateRequest.getPhone() != null) currentApplicant.setPhone(updateRequest.getPhone());
        if (updateRequest.getDob() != null) currentApplicant.setDob(updateRequest.getDob());
        if (updateRequest.getGender() != null)  currentApplicant.setGender(updateRequest.getGender());
        if (updateRequest.getEducation() != null) currentApplicant.setEducation(updateRequest.getEducation());
        if (updateRequest.getLevel() != null) currentApplicant.setLevel(updateRequest.getLevel());
        if (updateRequest.getAvailableStatus() != null) currentApplicant.setAvailableStatus(updateRequest.getAvailableStatus());
        if (updateRequest.getHeadline() != null) currentApplicant.setHeadline(updateRequest.getHeadline());
        if (updateRequest.getBio() != null) currentApplicant.setBio(updateRequest.getBio());

        if(updateRequest.getAvatar() != null) {
            String avatarUrl = BasicUtil.uploadImage(updateRequest.getAvatar(), "avatars", this.storageService);
            if(currentApplicant.getAvatar() != null) {
                String oldKey = currentApplicant.getAvatar().split(".amazonaws.com/")[1];
                this.storageService.handleDeleteFile(oldKey);
            }
            currentApplicant.setAvatar(avatarUrl);
        }
        if(updateRequest.getCoverPhoto() != null) {
            String coverPhotoUrl = BasicUtil.uploadImage(updateRequest.getCoverPhoto(), "user-cover-photos", this.storageService);
            if(currentApplicant.getCoverPhoto() != null) {
                String oldKey = currentApplicant.getCoverPhoto().split(".amazonaws.com/")[1];
                this.storageService.handleDeleteFile(oldKey);
            }
            currentApplicant.setCoverPhoto(coverPhotoUrl);
        }

        if(updateRequest.getAddresses() != null) {
            List<Address> addressList;
            try {
                addressList = this.mapper.readValue(
                        updateRequest.getAddresses(),
                        new TypeReference<List<Address>>() {}
                );
            } catch (JsonProcessingException e) {
                throw new InvalidException("Invalid address format");
            }

            List<Address> addresses = currentApplicant.getAddresses();
            Map<Long, Address> currentMap = addresses.stream()
                    .collect(Collectors.toMap(Address::getAddressId, Function.identity()));

            Set<Long> requestIds = addressList.stream()
                    .map(Address::getAddressId)
                    .filter(id -> id > 0)
                    .collect(Collectors.toSet());

            /* ================= DELETE ================= */
            Iterator<Address> iterator = addresses.iterator();
            while (iterator.hasNext()) {
                Address addr = iterator.next();
                if (!requestIds.contains(addr.getAddressId())) {
                    iterator.remove();
                    this.addressRepository.delete(addr);
                }
            }

            /* ================= UPDATE & CREATE ================= */
            for (Address reqAddr : addressList) {
                // ===== UPDATE =====
                if (reqAddr.getAddressId() > 0 && currentMap.containsKey(reqAddr.getAddressId())) {
                    Address currentAddr = currentMap.get(reqAddr.getAddressId());

                    if (!Objects.equals(currentAddr.getProvince(), reqAddr.getProvince()) ||
                            !Objects.equals(currentAddr.getFullAddress(), reqAddr.getFullAddress())) {
                        currentAddr.setProvince(reqAddr.getProvince());
                        currentAddr.setFullAddress(reqAddr.getFullAddress());
                    }
                }

                // ===== CREATE =====
                else if (reqAddr.getProvince() != null && reqAddr.getFullAddress() != null) {
                    Address newAddress = new Address();
                    newAddress.setProvince(reqAddr.getProvince());
                    newAddress.setFullAddress(reqAddr.getFullAddress());
                    newAddress.setAccount(currentApplicant);
                    addresses.add(newAddress);
                }
            }
        }

        Applicant savedApplicant = this.applicantRepository.save(currentApplicant);
        Applicant latestApplicant = this.applicantRepository.findById(savedApplicant.getAccountId()).orElse(savedApplicant);

        String currentEmail = SecurityUtil.getCurrentUserEmail();
        if (currentEmail != null && !currentEmail.isBlank()) {
            ApplicantResponse payload = this.convertToApplicantResponse(latestApplicant);
            this.profileRealtimeService.emitUserProfileUpdated(currentEmail, "APPLICANT", payload);
        }

        return latestApplicant;
    }

    @Override
    public Applicant handleGetApplicantById(long id) {
        return this.applicantRepository.findById(id).orElse(null);
    }

    @Override
    public Applicant handleToggleAvailableStatus() throws InvalidException {
        String email = SecurityUtil.getCurrentUserEmail();
        Applicant applicant = this.applicantRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new InvalidException("Applicant not found"));

        boolean currentStatus = applicant.isAvailableStatus();
        applicant.setAvailableStatus(!currentStatus);

        return this.applicantRepository.save(applicant);
    }

    @Override
    public ResultPaginationResponse handleGetAllApplicants(Specification<Applicant> spec, Pageable pageable) {
        Page<Applicant> page = this.applicantRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<ApplicantResponse> applicantResponses = page.getContent().stream()
                .map(this :: convertToApplicantResponse)
                .toList();

        return new ResultPaginationResponse(meta, applicantResponses);
    }

    @Override
    public ApplicantResponse convertToApplicantResponse(Applicant applicant) {
        ApplicantResponse applicantResponse = new ApplicantResponse();

        applicantResponse.setAccountId(applicant.getAccountId());
        applicantResponse.setUsername(applicant.getUsername());
        applicantResponse.setEmail(applicant.getEmail());
        applicantResponse.setPhone(applicant.getPhone());
        applicantResponse.setAddresses(applicant.getAddresses());
        applicantResponse.setFullName(applicant.getFullName());
        applicantResponse.setAvatar(applicant.getAvatar());
        applicantResponse.setGender(applicant.getGender());
        applicantResponse.setDob(applicant.getDob());
        applicantResponse.setEnabled(applicant.isEnabled());
        applicantResponse.setCoverPhoto(applicant.getCoverPhoto());
        applicantResponse.setHeadline(applicant.getHeadline());
        applicantResponse.setBio(applicant.getBio());
        applicantResponse.setCreatedAt(applicant.getCreatedAt());
        applicantResponse.setUpdatedAt(applicant.getUpdatedAt());
        applicantResponse.setEducation(applicant.getEducation());
        applicantResponse.setLevel(applicant.getLevel());
        applicantResponse.setAvailableStatus(applicant.isAvailableStatus());

        if (applicant.getRole() != null) {
            UserResponse.RoleAccount roleAccount = new UserResponse.RoleAccount();
            roleAccount.setRoleId(applicant.getRole().getRoleId());
            roleAccount.setName(applicant.getRole().getName());
            applicantResponse.setRole(roleAccount);
        }

        return applicantResponse;
    }
}
