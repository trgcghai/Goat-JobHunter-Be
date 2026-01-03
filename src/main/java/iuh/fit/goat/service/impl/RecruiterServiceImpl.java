package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.recruiter.RecruiterUpdateRequest;
import iuh.fit.goat.dto.response.recruiter.RecruiterResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.user.UserResponse;
import iuh.fit.goat.entity.Address;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.service.RecruiterService;
import iuh.fit.goat.service.RoleService;
import iuh.fit.goat.util.FileUploadUtil;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruiterServiceImpl implements RecruiterService {
    private final RecruiterRepository recruiterRepository;
//    private final JobRepository jobRepository;
//    private final CommentRepository commentRepository;
//    private final BlogRepository blogRepository;
//    private final NotificationRepository notificationRepository;
    private final AddressRepository addressRepository;
    private final RoleService roleService;
    private final String HR = "HR";

    @Override
    public Recruiter handleCreateRecruiter(Recruiter recruiter) {
        Role role;
        if(recruiter.getRole() != null) {
            role = this.roleService.handleGetRoleById(recruiter.getRole().getRoleId());
        } else {
            role = this.roleService.handleGetRoleByName(HR);
        }
        recruiter.setRole(role);
        recruiter.setEnabled(false);

        if(recruiter.getAvatar() == null) {
            recruiter.setAvatar(FileUploadUtil.AVATAR + recruiter.getUsername());
        }

        return this.recruiterRepository.save(recruiter);
    }

//    @Override
//    public void handleDeleteRecruiter(long id) {
//        Recruiter recruiter = this.handleGetRecruiterById(id);
//
//        if(recruiter != null){
//            if(recruiter.getJobs() != null){
//                List<Job> jobs = this.jobRepository.findByRecruiter(recruiter);
//                this.jobRepository.deleteAll(jobs);
//            }
//            if(recruiter.getUsers() != null){
//                recruiter.getUsers().forEach(user -> {
//                    user.getFollowedRecruiters().remove(recruiter);
//                });
//            }
//            if(recruiter.getBlogs() != null){
//                this.blogRepository.deleteAll(recruiter.getBlogs());
//            }
//            if(recruiter.getComments() != null){
//                this.commentRepository.deleteAll(recruiter.getComments());
//            }
//            if(recruiter.getActorNotifications() != null){
//                this.notificationRepository.deleteAll(recruiter.getActorNotifications());
//            }
//            if(recruiter.getRecipientNotifications() != null){
//                this.notificationRepository.deleteAll(recruiter.getRecipientNotifications());
//            }
//
//            this.recruiterRepository.deleteById(id);
//        }
//    }

    @Override
    public Recruiter handleUpdateRecruiter(RecruiterUpdateRequest updateRequest) {
        Recruiter currentRecruiter = this.handleGetRecruiterById(updateRequest.getAccountId());

        if (currentRecruiter == null) {
            return null;
        }

        if (updateRequest.getUsername() != null) {
            currentRecruiter.setUsername(updateRequest.getUsername());
        }
        if (updateRequest.getFullName() != null) {
            currentRecruiter.setFullName(updateRequest.getFullName());
        }
        if (updateRequest.getEmail() != null) {
            currentRecruiter.setEmail(updateRequest.getEmail());
        }
        if (updateRequest.getPhone() != null) {
            currentRecruiter.setPhone(updateRequest.getPhone());
        }
        // Handle addresses
        if (updateRequest.getAddresses() != null) {
            List<Address> currentAddresses = currentRecruiter.getAddresses();
            List<Address> requestAddresses = updateRequest.getAddresses();

            // Map current address theo addressId
            Map<Long, Address> currentMap = currentAddresses.stream()
                    .collect(Collectors.toMap(Address::getAddressId, Function.identity()));

            // Lấy danh sách addressId từ request
            Set<Long> requestIds = requestAddresses.stream()
                    .map(Address::getAddressId)
                    .filter(id -> id > 0)
                    .collect(Collectors.toSet());

            /* ================= DELETE ================= */
            Iterator<Address> iterator = currentAddresses.iterator();
            while (iterator.hasNext()) {
                Address addr = iterator.next();
                if (!requestIds.contains(addr.getAddressId())) {
                    iterator.remove();
                    this.addressRepository.delete(addr);
                }
            }

            /* ================= UPDATE & CREATE ================= */
            for (Address reqAddr : requestAddresses) {
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
                    currentAddresses.add(newAddress);
                }
            }
        }
        if (updateRequest.getDob() != null) {
            currentRecruiter.setDob(updateRequest.getDob());
        }
        if (updateRequest.getGender() != null) {
            currentRecruiter.setGender(updateRequest.getGender());
        }
        if (updateRequest.getPosition() != null) {
            currentRecruiter.setPosition(updateRequest.getPosition());
        }
        if (updateRequest.getAvatar() != null) {
            currentRecruiter.setAvatar(updateRequest.getAvatar());
        }

        return this.recruiterRepository.save(currentRecruiter);
    }


    @Override
    public Recruiter handleGetRecruiterById(long id) {
        Optional<Recruiter> recruiter = this.recruiterRepository.findById(id);

        return recruiter.orElse(null);
    }

//    @Override
//    public Recruiter handleGetCurrentRecruiter() {
//        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
//        if (email == null) {
//            return null;
//        }
//        return this.recruiterRepository.findByContactEmail(email).orElse(null);
//    }
//
//
//    @Override
//    public ResultPaginationResponse handleGetAllRecruiters(Specification<Recruiter> spec, Pageable pageable) {
//        Page<Recruiter> page = this.recruiterRepository.findAll(spec, pageable);
//
//        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
//        meta.setPage(pageable.getPageNumber() + 1);
//        meta.setPageSize(pageable.getPageSize());
//        meta.setPages(page.getTotalPages());
//        meta.setTotal(page.getTotalElements());
//
//        List<RecruiterResponse> recruiterResponses = page.getContent().stream()
//                .map(this :: convertToRecruiterResponse)
//                .toList();
//
//        return new ResultPaginationResponse(meta, recruiterResponses);
//    }
//
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
            UserResponse.RoleUser roleUser = new UserResponse.RoleUser();
            roleUser.setRoleId(recruiter.getRole().getRoleId());
            roleUser.setName(recruiter.getRole().getName());
            recruiterResponse.setRole(roleUser);
        }

        return recruiterResponse;
    }
}
