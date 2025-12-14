package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.recruiter.RecruiterUpdateRequest;
import iuh.fit.goat.dto.response.recruiter.RecruiterResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.user.UserResponse;
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

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecruiterServiceImpl implements RecruiterService {
//    private final RecruiterRepository recruiterRepository;
//    private final JobRepository jobRepository;
//    private final CommentRepository commentRepository;
//    private final BlogRepository blogRepository;
//    private final NotificationRepository notificationRepository;
//    private final RoleService roleService;
//    private final String HR = "HR";
//
//    @Override
//    public Recruiter handleCreateRecruiter(Recruiter recruiter) {
//        Role role;
//        if(recruiter.getRole() != null) {
//            role = this.roleService.handleGetRoleById(recruiter.getRole().getRoleId());
//        } else {
//            role = this.roleService.handleGetRoleByName(HR);
//        }
//        recruiter.setRole(role);
//        recruiter.setEnabled(false);
//
//        if(recruiter.getAvatar() == null) {
//            recruiter.setAvatar(FileUploadUtil.AVATAR + recruiter.getUsername());
//        }
//
//        return this.recruiterRepository.save(recruiter);
//    }
//
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
//
//    @Override
//    public Recruiter handleUpdateRecruiter(RecruiterUpdateRequest updateRequest) {
//        Recruiter currentRecruiter = this.handleGetRecruiterById(updateRequest.getUserId());
//
//        if (currentRecruiter == null) {
//            return null;
//        }
//
//        if (updateRequest.getUsername() != null) {
//            currentRecruiter.setUsername(updateRequest.getUsername());
//        }
//        if (updateRequest.getFullName() != null) {
//            currentRecruiter.setFullName(updateRequest.getFullName());
//        }
//        if (updateRequest.getContact() != null) {
//            currentRecruiter.setContact(updateRequest.getContact());
//        }
//        if (updateRequest.getAddress() != null) {
//            currentRecruiter.setAddress(updateRequest.getAddress());
//        }
//        if (updateRequest.getDob() != null) {
//            currentRecruiter.setDob(updateRequest.getDob());
//        }
//        if (updateRequest.getGender() != null) {
//            currentRecruiter.setGender(updateRequest.getGender());
//        }
//        if (updateRequest.getDescription() != null) {
//            currentRecruiter.setDescription(updateRequest.getDescription());
//        }
//        if (updateRequest.getWebsite() != null) {
//            currentRecruiter.setWebsite(updateRequest.getWebsite());
//        }
//        if (updateRequest.getAvatar() != null) {
//            currentRecruiter.setAvatar(updateRequest.getAvatar());
//        }
//
//        return this.recruiterRepository.save(currentRecruiter);
//    }
//
//
//    @Override
//    public Recruiter handleGetRecruiterById(long id) {
//        Optional<Recruiter> recruiter = this.recruiterRepository.findById(id);
//
//        return recruiter.orElse(null);
//
//    }
//
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
//    @Override
//    public RecruiterResponse convertToRecruiterResponse(Recruiter recruiter) {
//        RecruiterResponse recruiterResponse = new RecruiterResponse();
//
//        recruiterResponse.setUserId(recruiter.getUserId());
//        recruiterResponse.setContact(recruiter.getContact());
//        recruiterResponse.setAddress(recruiter.getAddress());
//        recruiterResponse.setUsername(recruiter.getUsername());
//        recruiterResponse.setFullName(recruiter.getFullName());
//        recruiterResponse.setCreatedAt(recruiter.getCreatedAt());
//        recruiterResponse.setUpdatedAt(recruiter.getUpdatedAt());
//        recruiterResponse.setDescription(recruiter.getDescription());
//        recruiterResponse.setWebsite(recruiter.getWebsite());
//        recruiterResponse.setAvatar(recruiter.getAvatar());
//        recruiterResponse.setEnabled(recruiter.isEnabled());
//
//        if(recruiter.getRole() != null) {
//            UserResponse.RoleUser roleUser = new UserResponse.RoleUser();
//            roleUser.setRoleId(recruiter.getRole().getRoleId());
//            roleUser.setName(recruiter.getRole().getName());
//
//            recruiterResponse.setRole(roleUser);
//        }
//
//        return recruiterResponse;
//    }
}
