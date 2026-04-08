package iuh.fit.goat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.goat.dto.request.recruiter.RecruiterUpdateRequest;
import iuh.fit.goat.dto.response.recruiter.RecruiterResponse;
import iuh.fit.goat.dto.response.user.UserResponse;
import iuh.fit.goat.entity.Address;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.enumeration.Gender;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.service.ProfileRealtimeService;
import iuh.fit.goat.service.RecruiterService;
import iuh.fit.goat.service.RoleService;
import iuh.fit.goat.service.StorageService;
import iuh.fit.goat.util.BasicUtil;
import iuh.fit.goat.util.FileUploadUtil;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruiterServiceImpl implements RecruiterService {
    private final StorageService storageService;
    private final ProfileRealtimeService profileRealtimeService;

    private final RecruiterRepository recruiterRepository;
    private final AddressRepository addressRepository;

    private final ObjectMapper mapper;
    private final String HR = "HR";

    @Override
    public Recruiter handleCreateRecruiter(Recruiter recruiter) throws InvalidException {
        recruiter.setEnabled(false);
        if (recruiter.getAvatar() == null) {
            try {
                MultipartFile multipartFile = BasicUtil.convertToMultipartFile(
                        recruiter.getGender() == Gender.MALE ? FileUploadUtil.AVATAR_MALE : FileUploadUtil.AVATAR_FEMALE
                );
                String avatarUrl = BasicUtil.uploadImage(multipartFile, "avatars", this.storageService);
                recruiter.setAvatar(avatarUrl);
            } catch (IOException | InvalidException e) {
                throw new InvalidException("Failed to set default avatar");
            }
        }

        return this.recruiterRepository.save(recruiter);
    }

    @Override
    public Recruiter handleUpdateRecruiter(RecruiterUpdateRequest updateRequest) throws InvalidException {
        Recruiter currentRecruiter = this.handleGetRecruiterById(updateRequest.getAccountId());
        if (currentRecruiter == null) return null;

        if (updateRequest.getUsername() != null) currentRecruiter.setUsername(updateRequest.getUsername());
        if (updateRequest.getFullName() != null) currentRecruiter.setFullName(updateRequest.getFullName());
        if (updateRequest.getEmail() != null) currentRecruiter.setEmail(updateRequest.getEmail());
        if (updateRequest.getPhone() != null) currentRecruiter.setPhone(updateRequest.getPhone());
        if (updateRequest.getDob() != null) currentRecruiter.setDob(updateRequest.getDob());
        if (updateRequest.getGender() != null) currentRecruiter.setGender(updateRequest.getGender());
        if (updateRequest.getHeadline() != null) currentRecruiter.setHeadline(updateRequest.getHeadline());
        if (updateRequest.getBio() != null) currentRecruiter.setBio(updateRequest.getBio());
        if (updateRequest.getPosition() != null) currentRecruiter.setPosition(updateRequest.getPosition());

        if(updateRequest.getAvatar() != null) {
            String avatarUrl = BasicUtil.uploadImage(updateRequest.getAvatar(), "avatars", this.storageService);
            if(currentRecruiter.getAvatar() != null) {
                String oldKey = currentRecruiter.getAvatar().split(".amazonaws.com/")[1];
                this.storageService.handleDeleteFile(oldKey);
            }
            currentRecruiter.setAvatar(avatarUrl);
        }
        if(updateRequest.getCoverPhoto() != null) {
            String coverPhotoUrl = BasicUtil.uploadImage(updateRequest.getCoverPhoto(), "user-cover-photos", this.storageService);
            if(currentRecruiter.getCoverPhoto() != null) {
                String oldKey = currentRecruiter.getCoverPhoto().split(".amazonaws.com/")[1];
                this.storageService.handleDeleteFile(oldKey);
            }
            currentRecruiter.setCoverPhoto(coverPhotoUrl);
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

            List<Address> addresses = currentRecruiter.getAddresses();
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
                    newAddress.setAccount(currentRecruiter);
                    addresses.add(newAddress);
                }
            }
        }

        Recruiter savedRecruiter = this.recruiterRepository.save(currentRecruiter);
        Recruiter latestRecruiter = this.recruiterRepository.findById(savedRecruiter.getAccountId()).orElse(savedRecruiter);

        String currentEmail = SecurityUtil.getCurrentUserEmail();
        if (currentEmail != null && !currentEmail.isBlank()) {
            RecruiterResponse payload = this.convertToRecruiterResponse(latestRecruiter);
            this.profileRealtimeService.emitUserProfileUpdated(currentEmail, "RECRUITER", payload);
        }

        return latestRecruiter;
    }


    @Override
    public Recruiter handleGetRecruiterById(long id) {
        Optional<Recruiter> recruiter = this.recruiterRepository.findById(id);

        return recruiter.orElse(null);
    }

    @Override
    public RecruiterResponse convertToRecruiterResponse(Recruiter recruiter) {
        RecruiterResponse recruiterResponse = new RecruiterResponse();

        recruiterResponse.setAccountId(recruiter.getAccountId());
        recruiterResponse.setUsername(recruiter.getUsername());
        recruiterResponse.setEmail(recruiter.getEmail());
        recruiterResponse.setPhone(recruiter.getPhone());
        recruiterResponse.setAddresses(recruiter.getAddresses());
        recruiterResponse.setFullName(recruiter.getFullName());
        recruiterResponse.setAvatar(recruiter.getAvatar());
        recruiterResponse.setGender(recruiter.getGender());
        recruiterResponse.setDob(recruiter.getDob());
        recruiterResponse.setEnabled(recruiter.isEnabled());
        recruiterResponse.setCoverPhoto(recruiter.getCoverPhoto());
        recruiterResponse.setHeadline(recruiter.getHeadline());
        recruiterResponse.setBio(recruiter.getBio());
        recruiterResponse.setCreatedAt(recruiter.getCreatedAt());
        recruiterResponse.setUpdatedAt(recruiter.getUpdatedAt());
        recruiterResponse.setPosition(recruiter.getPosition());

        if(recruiter.getCompany() != null) {
            RecruiterResponse.CompanySummary companySummary = new RecruiterResponse.CompanySummary();
            companySummary.setCompanyId(recruiter.getCompany().getAccountId());
            companySummary.setName(recruiter.getCompany().getName());
            recruiterResponse.setCompany(companySummary);
        }

        if(recruiter.getRole() != null) {
            UserResponse.RoleAccount roleAccount = new UserResponse.RoleAccount();
            roleAccount.setRoleId(recruiter.getRole().getRoleId());
            roleAccount.setName(recruiter.getRole().getName());
            recruiterResponse.setRole(roleAccount);
        }

        return recruiterResponse;
    }
}
