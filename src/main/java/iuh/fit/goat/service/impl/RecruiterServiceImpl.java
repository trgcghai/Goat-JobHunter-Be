package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.RecruiterResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.UserResponse;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.util.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RecruiterServiceImpl implements iuh.fit.goat.service.RecruiterService {
    private final RecruiterRepository recruiterRepository;
    private final JobRepository jobRepository;
    private final CommentRepository commentRepository;
    private final BlogRepository blogRepository;
    private final NotificationRepository notificationRepository;
    private final String HR = "HR";

    public RecruiterServiceImpl(RecruiterRepository recruiterRepository, JobRepository jobRepository,
                            CommentRepository commentRepository,
                            BlogRepository blogRepository, NotificationRepository notificationRepository
    ) {
        this.recruiterRepository = recruiterRepository;
        this.jobRepository = jobRepository;
        this.commentRepository = commentRepository;
        this.blogRepository = blogRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Recruiter handleCreateRecruiter(Recruiter recruiter) {
        Role role = null;
        recruiter.setRole(role);
        recruiter.setVerificationCode(SecurityUtil.generateVerificationCode());
        recruiter.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        recruiter.setEnabled(false);

        return this.recruiterRepository.save(recruiter);
    }

    @Override
    public void handleDeleteRecruiter(long id) {
        Recruiter recruiter = this.handleGetRecruiterById(id);

        if(recruiter != null){
            if(recruiter.getJobs() != null){
                List<Job> jobs = this.jobRepository.findByRecruiter(recruiter);
                this.jobRepository.deleteAll(jobs);
            }
            if(recruiter.getUsers() != null){
                recruiter.getUsers().forEach(user -> {
                    user.getFollowedRecruiters().remove(recruiter);
                });
            }
            if(recruiter.getBlogs() != null){
                this.blogRepository.deleteAll(recruiter.getBlogs());
            }
            if(recruiter.getComments() != null){
                this.commentRepository.deleteAll(recruiter.getComments());
            }
            if(recruiter.getActorNotifications() != null){
                this.notificationRepository.deleteAll(recruiter.getActorNotifications());
            }
            if(recruiter.getRecipientNotifications() != null){
                this.notificationRepository.deleteAll(recruiter.getRecipientNotifications());
            }

            this.recruiterRepository.deleteById(id);
        }
    }

    @Override
    public Recruiter handleUpdateRecruiter(Recruiter updateRecruiter) {
        Recruiter currentRecruiter = this.handleGetRecruiterById(updateRecruiter.getUserId());

        if(currentRecruiter != null) {
            currentRecruiter.setAddress(updateRecruiter.getAddress());
            currentRecruiter.setContact(updateRecruiter.getContact());
            currentRecruiter.setDob(updateRecruiter.getDob());
            currentRecruiter.setFullName(updateRecruiter.getFullName());
            currentRecruiter.setGender(updateRecruiter.getGender());
            currentRecruiter.setUsername(updateRecruiter.getUsername());
            currentRecruiter.setDescription(updateRecruiter.getDescription());
            currentRecruiter.setAvatar(updateRecruiter.getAvatar());
            currentRecruiter.setWebsite(updateRecruiter.getWebsite());
            currentRecruiter.setEnabled(updateRecruiter.isEnabled());

            return this.recruiterRepository.save(currentRecruiter);
        }

        return null;
    }

    @Override
    public Recruiter handleGetRecruiterById(long id) {
        Optional<Recruiter> recruiter = this.recruiterRepository.findById(id);

        if(recruiter.isPresent()) {
            return recruiter.get();
        }

        return null;
    }

    @Override
    public ResultPaginationResponse handleGetAllRecruiters(Specification<Recruiter> spec, Pageable pageable) {
        Page<Recruiter> page = this.recruiterRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<RecruiterResponse> recruiterResponses = page.getContent().stream()
                .map(this :: convertToRecruiterResponse)
                .toList();

        return new ResultPaginationResponse(meta, recruiterResponses);
    }

    @Override
    public RecruiterResponse convertToRecruiterResponse(Recruiter recruiter) {
        RecruiterResponse recruiterResponse = new RecruiterResponse();

        recruiterResponse.setUserId(recruiter.getUserId());
        recruiterResponse.setContact(recruiter.getContact());
        recruiterResponse.setAddress(recruiter.getAddress());
        recruiterResponse.setUsername(recruiter.getUsername());
        recruiterResponse.setFullName(recruiter.getFullName());
        recruiterResponse.setCreatedAt(recruiter.getCreatedAt());
        recruiterResponse.setUpdatedAt(recruiter.getUpdatedAt());
        recruiterResponse.setDescription(recruiter.getDescription());
        recruiterResponse.setWebsite(recruiter.getWebsite());
        recruiterResponse.setAvatar(recruiter.getAvatar());
        recruiterResponse.setEnabled(recruiter.isEnabled());

        if(recruiter.getRole() != null) {
            UserResponse.RoleUser roleUser = new UserResponse.RoleUser();
            roleUser.setRoleId(recruiter.getRole().getRoleId());
            roleUser.setName(recruiter.getRole().getName());

            recruiterResponse.setRole(roleUser);
        }

        return recruiterResponse;
    }
}
