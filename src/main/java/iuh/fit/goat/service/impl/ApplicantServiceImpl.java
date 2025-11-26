package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.ApplicantUpdateRequest;
import iuh.fit.goat.dto.response.ApplicantResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.UserResponse;
import iuh.fit.goat.service.ApplicantService;
import iuh.fit.goat.service.EmailService;
import iuh.fit.goat.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.util.*;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApplicantServiceImpl implements ApplicantService {
    private final ApplicantRepository applicantRepository;
    private final ApplicationRepository applicationRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final RoleService roleService;
    private final EmailService emailService;
    private final String APPLICANT = "APPLICANT";

    @Override
    public Applicant handleCreateApplicant(Applicant applicant) {
        Role role;
        if(applicant.getRole() != null){
            role = this.roleService.handleGetRoleById(applicant.getRole().getRoleId());
        } else {
            role = this.roleService.handleGetRoleByName(APPLICANT);
        }
        applicant.setRole(role);
        applicant.setEnabled(false);

        return this.applicantRepository.save(applicant);
    }

    @Override
    public void handleDeleteApplicant(long id) {
        Applicant applicant = this.handleGetApplicantById(id);
        if(applicant.getApplications() != null){
            List<Application> applications = this.applicationRepository.findByApplicant(applicant);
            this.applicationRepository.deleteAll(applications);
        }
        if(applicant.getComments() != null){
            this.commentRepository.deleteAll(applicant.getComments());
        }
        if(applicant.getActorNotifications() != null){
            List<Notification> notifications = applicant.getActorNotifications();
            this.notificationRepository.deleteAll(notifications);
        }
        if(applicant.getRecipientNotifications() != null){
            List<Notification> notifications = applicant.getRecipientNotifications();
            this.notificationRepository.deleteAll(notifications);
        }

        this.applicantRepository.deleteById(id);
    }

    @Override
    public Applicant handleUpdateApplicant(ApplicantUpdateRequest updateRequest) {
        Applicant currentApplicant = this.handleGetApplicantById(updateRequest.getUserId());

        if (currentApplicant == null) {
            return null;
        }

        if (updateRequest.getUsername() != null) {
            currentApplicant.setUsername(updateRequest.getUsername());
        }
        if (updateRequest.getFullName() != null) {
            currentApplicant.setFullName(updateRequest.getFullName());
        }
        if (updateRequest.getContact() != null) {
            currentApplicant.setContact(updateRequest.getContact());
        }
        if (updateRequest.getAddress() != null) {
            currentApplicant.setAddress(updateRequest.getAddress());
        }
        if (updateRequest.getEducation() != null) {
            currentApplicant.setEducation(updateRequest.getEducation());
        }
        if (updateRequest.getLevel() != null) {
            currentApplicant.setLevel(updateRequest.getLevel());
        }
        if (updateRequest.getAvatar() != null) {
            currentApplicant.setAvatar(updateRequest.getAvatar());
        }

        return this.applicantRepository.save(currentApplicant);
    }


    @Override
    public Applicant handleGetApplicantById(long id) {
        Optional<Applicant> result = this.applicantRepository.findById(id);

        return result.orElse(null);
    }

    @Override
    public Applicant handleGetCurrentApplicant() {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) {
            return null;
        }
        return this.applicantRepository.findByContactEmail(email).orElse(null);
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

        applicantResponse.setUserId(applicant.getUserId());
        applicantResponse.setContact(applicant.getContact());
        applicantResponse.setAddress(applicant.getAddress());
        applicantResponse.setUsername(applicant.getUsername());
        applicantResponse.setFullName(applicant.getFullName());
        applicantResponse.setCreatedAt(applicant.getCreatedAt());
        applicantResponse.setUpdatedAt(applicant.getUpdatedAt());
        applicantResponse.setEducation(applicant.getEducation());
        applicantResponse.setLevel(applicant.getLevel());
        applicantResponse.setGender(applicant.getGender());
        applicantResponse.setDob(applicant.getDob());
        applicantResponse.setAvailableStatus(applicant.isAvailableStatus());
        applicantResponse.setAvatar(applicant.getAvatar());
        applicantResponse.setEnabled(applicant.isEnabled());

        if(applicant.getRole() != null) {
            UserResponse.RoleUser roleUser = new UserResponse.RoleUser();
            roleUser.setRoleId(applicant.getRole().getRoleId());
            roleUser.setName(applicant.getRole().getName());

            applicantResponse.setRole(roleUser);
        }

        return applicantResponse;
    }
}
