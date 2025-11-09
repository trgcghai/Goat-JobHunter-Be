package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.Role;
import iuh.fit.goat.dto.request.FollowRecruiterRequest;
import iuh.fit.goat.dto.request.ResetPasswordRequest;
import iuh.fit.goat.dto.request.SaveJobRequest;
import iuh.fit.goat.dto.response.LoginResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.UserResponse;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.JobRepository;
import iuh.fit.goat.repository.RecruiterRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.RedisService;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final RecruiterRepository recruiterRepository;
    private final RedisService redisService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;

    @Value("${minhdat.jwt.refresh-token-validity-in-seconds}")
    private long jwtRefreshToken;

    @Override
    public User handleGetUserByEmail(String email) {
        return this.userRepository.findByContact_Email(email);
    }

    @Override
    public boolean handleExistsByEmail(String email) {
        return this.userRepository.existsByContact_Email(email);
    }

    @Override
    public User handleGetUserById(long id) {
        return this.userRepository.findById(id).orElse(null);
    }

    @Override
    public ResultPaginationResponse handleGetAllUsers (Specification<User> spec, Pageable pageable) {
        Page<User> page = this.userRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<UserResponse> userResponses = page.getContent().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());

        return new ResultPaginationResponse(meta, userResponses);
    }

    @Override
    public boolean handleCheckCurrentPassword(String currentPassword) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if(!currentEmail.isEmpty()) {
            User currentUser = this.handleGetUserByEmail(currentEmail);
            return passwordEncoder.matches(currentPassword, currentUser.getPassword());
        }

        return false;
    }

    @Override
    public Map<String, Object> handleUpdatePassword(String newPassword, String refreshToken) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if(!currentEmail.isEmpty()) {
            User currentUser = this.handleGetUserByEmail(currentEmail);
            String hashedPassword = this.passwordEncoder.encode(newPassword);
            currentUser.setPassword(hashedPassword);
            User res = this.userRepository.save(currentUser);

            LoginResponse loginResponse = new LoginResponse();
            LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin(
                    res.getUserId(), res.getContact().getEmail(),
                    res.getFullName(), res.getUsername(), res.getAvatar(),
                    res instanceof Applicant ? Role.APPLICANT.getValue() : Role.RECRUITER.getValue(),
                    res.isEnabled(),
                    res.getRole(), res.getSavedJobs(), res.getFollowedRecruiters(),
                    res.getActorNotifications()
            );
            loginResponse.setUser(userLogin);
            String newAccessToken = this.securityUtil.createAccessToken(currentEmail, loginResponse);
            String newRefreshToken = this.securityUtil.createRefreshToken(currentEmail, loginResponse);
            this.redisService.replaceToken(
                    refreshToken, newRefreshToken,
                    currentEmail, jwtRefreshToken, TimeUnit.SECONDS

            );

            Map<String, Object> response = new HashMap<>();
            response.put("loginResponse", loginResponse);
            response.put("refreshToken", newRefreshToken);
            response.put("accessToken", newAccessToken);

            return response;
        }

        return null;
    }

    @Override
    public void handleResetPassword(ResetPasswordRequest resetPasswordRequest) throws InvalidException {
        User user = this.handleGetUserByEmail(resetPasswordRequest.getEmail());
        if(user != null) {
            String hashedPassword = this.passwordEncoder.encode(resetPasswordRequest.getNewPassword());
            user.setPassword(hashedPassword);
            this.userRepository.save(user);
        } else {
            throw new InvalidException("User not found");
        }
    }

    @Override
    public UserResponse handleSaveJobs(SaveJobRequest saveJobRequest) {
        User user = this.userRepository.findById(saveJobRequest.getUserId()).orElse(null);

        if(user != null) {
            List<Long> jobIds = saveJobRequest.getSavedJobs().stream().map(Job::getJobId).toList();
            List<Job> savedJobs = this.jobRepository.findByJobIdIn(jobIds);
            user.setSavedJobs(savedJobs);
            this.userRepository.save(user);

            return this.convertToUserResponse(user);
        }

        return null;
    }

    @Override
    public UserResponse handleFollowRecruiters(FollowRecruiterRequest followRecruiterRequest) {
        User user = this.userRepository.findById(followRecruiterRequest.getUserId()).orElse(null);

        if(user != null) {
            List<Long> recruiterIds = followRecruiterRequest.getFollowedRecruiters()
                    .stream().map(Recruiter::getUserId).toList();
            List<Recruiter> followedRecruiters = this.recruiterRepository.findByUserIdIn(recruiterIds);
            user.setFollowedRecruiters(followedRecruiters);
            this.userRepository.save(user);

            return this.convertToUserResponse(user);
        }

        return null;
    }

    @Override
    public UserResponse convertToUserResponse(User user) {
        UserResponse userResponse = new UserResponse();

        userResponse.setUserId(user.getUserId());
        userResponse.setContact(user.getContact());
        userResponse.setAddress(user.getAddress());
        userResponse.setUsername(user.getUsername());
        userResponse.setFullName(user.getFullName());
        userResponse.setAvatar(user.getAvatar());
        userResponse.setCreatedAt(user.getCreatedAt());
        userResponse.setUpdatedAt(user.getUpdatedAt());


        if(user.getRole() != null) {
            UserResponse.RoleUser roleUser = new UserResponse.RoleUser();
            roleUser.setRoleId(user.getRole().getRoleId());
            roleUser.setName(user.getRole().getName());

            userResponse.setRole(roleUser);
        }
        if(user.getSavedJobs() != null) {
            List<UserResponse.SavedJob> savedJobs = new ArrayList<>();
            user.getSavedJobs().forEach(savedJob -> {
                savedJobs.add(new UserResponse.SavedJob(savedJob.getJobId(), savedJob.getTitle()));
            });
            userResponse.setSavedJobs(savedJobs);
        }
        if(user.getFollowedRecruiters() != null) {
            List<UserResponse.FollowedRecruiter> followedRecruiters = new ArrayList<>();
            user.getFollowedRecruiters().forEach(followedRecruiter -> {
                followedRecruiters.add(new UserResponse.FollowedRecruiter(
                        followedRecruiter.getUserId(), followedRecruiter.getFullName()
                ));
            });
            userResponse.setFollowedRecruiters(followedRecruiters);
        }

        return userResponse;
    }

}
