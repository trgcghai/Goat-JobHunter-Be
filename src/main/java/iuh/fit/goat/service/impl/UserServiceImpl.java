package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.user.CreateUserRequest;
import iuh.fit.goat.dto.response.blog.BlogResponse;
import iuh.fit.goat.dto.response.interview.InterviewResponse;
import iuh.fit.goat.entity.embeddable.Contact;
import iuh.fit.goat.dto.request.user.ResetPasswordRequest;
import iuh.fit.goat.dto.response.auth.LoginResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.user.UserEnabledResponse;
import iuh.fit.goat.dto.response.user.UserResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.service.*;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // avoid circular dependency between user service impl, ai service impl and blog service impl
    @Lazy
    @Autowired
    private final BlogService blogService;
    private final InterviewService interviewService;

//    private final RedisService redisService;
    private final NotificationService notificationService;
//    private final EmailNotificationService emailNotificationService;

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;
    private final BlogRepository blogRepository;
    private final InterviewRepository interviewRepository;
//
//    private final PasswordEncoder passwordEncoder;
//    private final SecurityUtil securityUtil;
//    private final RoleRepository roleRepository;
//
//    @Value("${minhdat.jwt.refresh-token-validity-in-seconds}")
//    private long jwtRefreshToken;
//    @Value("${minhdat.verify-code-validity-in-seconds}")
//    private long validityInSeconds;
//
    @Override
    public User handleGetUserByEmail(String email) {
        return this.userRepository.findByEmailWithRole(email).orElse(null);
    }

    @Override
    public boolean handleExistsByEmail(String email) {
        return this.userRepository.existsByEmail(email);
    }
//
//    @Override
//    public User handleGetUserById(long id) {
//        return this.userRepository.findById(id).orElse(null);
//    }
//
//    @Override
//    public User handleCreateUser(CreateUserRequest request) throws InvalidException {
//        // Check if email already exists
//        if (this.handleExistsByEmail(request.getEmail())) {
//            throw new InvalidException("Email already exists");
//        }
//
//        // Get role entity
//        iuh.fit.goat.entity.Role roleEntity;
//        User newUser;
//
//        String hashedPassword = this.passwordEncoder.encode("11111111");
//
//        if (Role.APPLICANT.getValue().equalsIgnoreCase(request.getRole())) {
//            Applicant applicant = new Applicant();
//            applicant.setPassword(hashedPassword);
//            applicant.setEnabled(true);
//            applicant.setFullName(request.getFullName());
//            applicant.setUsername(request.getUsername());
//            applicant.setAddress(request.getAddress());
//
//            Contact contact = new Contact();
//            contact.setEmail(request.getEmail());
//            contact.setPhone(request.getPhone());
//            applicant.setContact(contact);
//
//            // Set role - you need to fetch from RoleRepository
//            // Assuming you have a RoleRepository injected
//             roleEntity = roleRepository.findByName(Role.APPLICANT.getValue());
//             applicant.setRole(roleEntity);
//
//            newUser = applicant;
//        } else if (Role.RECRUITER.getValue().equalsIgnoreCase(request.getRole())) {
//            Recruiter recruiter = new Recruiter();
//            recruiter.setPassword(hashedPassword);
//            recruiter.setEnabled(true);
//
//            if (recruiter.getAddress() == null || recruiter.getAddress().isEmpty()) {
//                recruiter.setAddress("Chưa cung cấp");
//            } else {
//                recruiter.setAddress(request.getAddress());
//            }
//
//            if (recruiter.getFullName() == null || recruiter.getFullName().isEmpty()) {
//                recruiter.setFullName("Chưa cung cấp");
//            } else {
//                recruiter.setFullName(request.getFullName());
//            }
//
//            if (recruiter.getUsername() == null || recruiter.getUsername().isEmpty()) {
//                recruiter.setUsername("Chưa cung cấp");
//            } else {
//                recruiter.setUsername(request.getUsername());
//            }
//
//            Contact contact = new Contact();
//            contact.setEmail(request.getEmail());
//            contact.setPhone(request.getPhone());
//            recruiter.setContact(contact);
//
//            // Set role
//             roleEntity = roleRepository.findByName(Role.RECRUITER.getValue());
//             recruiter.setRole(roleEntity);
//
//            newUser = recruiter;
//        } else {
//            throw new InvalidException("Invalid role. Must be APPLICANT or RECRUITER");
//        }
//
//        return this.userRepository.save(newUser);
//    }
//
//    @Override
//    public ResultPaginationResponse handleGetAllUsers(Specification<User> spec, Pageable pageable) {
//        Page<User> page = this.userRepository.findAll(spec, pageable);
//
//        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
//        meta.setPage(pageable.getPageNumber() + 1);
//        meta.setPageSize(pageable.getPageSize());
//        meta.setPages(page.getTotalPages());
//        meta.setTotal(page.getTotalElements());
//
//        List<UserResponse> userResponses = page.getContent().stream()
//                .map(this::convertToUserResponse)
//                .collect(Collectors.toList());
//
//        return new ResultPaginationResponse(meta, userResponses);
//    }
//
//    @Override
//    public boolean handleCheckCurrentPassword(String currentPassword) {
//        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
//                SecurityUtil.getCurrentUserLogin().get() : "";
//
//        if (!currentEmail.isEmpty()) {
//            User currentUser = this.handleGetUserByEmail(currentEmail);
//            return passwordEncoder.matches(currentPassword, currentUser.getPassword());
//        }
//
//        return false;
//    }
//
//    @Override
//    public Map<String, Object> handleUpdatePassword(String newPassword, String refreshToken) {
//        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
//                SecurityUtil.getCurrentUserLogin().get() : "";
//
//        if (!currentEmail.isEmpty()) {
//            User currentUser = this.handleGetUserByEmail(currentEmail);
//            String hashedPassword = this.passwordEncoder.encode(newPassword);
//            currentUser.setPassword(hashedPassword);
//            User res = this.userRepository.save(currentUser);
//
//            LoginResponse loginResponse = new LoginResponse();
//            LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin();
//
//            userLogin.setUserId(res.getUserId());
//            userLogin.setDob(res.getDob());
//            userLogin.setGender(res.getGender());
//            userLogin.setFullName(res.getFullName());
//            userLogin.setUsername(res.getUsername());
//            userLogin.setContact(res.getContact());
//            userLogin.setAvatar(res.getAvatar());
//            userLogin.setType(res instanceof Applicant ? Role.APPLICANT.getValue() : Role.RECRUITER.getValue());
//            userLogin.setRole(res.getRole());
//            userLogin.setEnabled(res.isEnabled());
//
//            loginResponse.setUser(userLogin);
//            String newAccessToken = this.securityUtil.createAccessToken(currentEmail, loginResponse);
//            String newRefreshToken = this.securityUtil.createRefreshToken(currentEmail, loginResponse);
//            this.redisService.replaceKey(
//                    "refresh:" + refreshToken,
//                    "refresh:" + newRefreshToken,
//                    currentEmail, jwtRefreshToken, TimeUnit.SECONDS
//
//            );
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("loginResponse", loginResponse);
//            response.put("refreshToken", newRefreshToken);
//            response.put("accessToken", newAccessToken);
//
//            return response;
//        }
//
//        return null;
//    }
//
//    @Override
//    public void handleResetPassword(ResetPasswordRequest resetPasswordRequest) throws InvalidException {
//        User user = this.handleGetUserByEmail(resetPasswordRequest.getEmail());
//        if (user != null) {
//            String hashedPassword = this.passwordEncoder.encode(resetPasswordRequest.getNewPassword());
//            user.setPassword(hashedPassword);
//            user.setEnabled(false);
//            this.userRepository.save(user);
//
//            String verificationCode = SecurityUtil.generateVerificationCode();
//            this.redisService.saveWithTTL(
//                    user.getContact().getEmail(),
//                    verificationCode,
//                    validityInSeconds,
//                    TimeUnit.SECONDS
//            );
//            this.emailNotificationService.handleSendVerificationEmail(user.getContact().getEmail(), verificationCode);
//        } else {
//            throw new InvalidException("User not found");
//        }
//    }


    /*     ========================= Saved Job Related Methods =========================  */

    @Override
    public ResultPaginationResponse handleGetCurrentUserSavedJobs(Pageable pageable) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null || currentUser.getSavedJobs() == null) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        List<Job> savedJobs = currentUser.getSavedJobs();
        int total = savedJobs.size();
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int start = pageNumber * pageSize;
        List<Job> content;

        if (start >= total || pageSize <= 0) {
            content = new ArrayList<>();
        } else {
            int end = Math.min(start + pageSize, total);
            content = savedJobs.subList(start, end);
        }

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageNumber + 1);
        meta.setPageSize(pageSize);
        int pages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
        meta.setPages(pages);
        meta.setTotal((long) total);

        return new ResultPaginationResponse(meta, content);
    }

    @Override
    public List<Map<String, Object>> handleCheckJobsSaved(List<Long> jobIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return new ArrayList<>();
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        List<Long> savedJobIds = currentUser.getSavedJobs() != null
                ? currentUser.getSavedJobs().stream().map(Job::getJobId).toList()
                : new ArrayList<>();

        return jobIds.stream()
                .map(jobId -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("jobId", jobId);
                    result.put("result", savedJobIds.contains(jobId));
                    return result;
                })
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse handleSaveJobsForCurrentUser(List<Long> jobIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return null;
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null) {
            return null;
        }

        List<Job> currentSavedJobs = currentUser.getSavedJobs() != null
                ? new ArrayList<>(currentUser.getSavedJobs())
                : new ArrayList<>();

        List<Job> jobsToAdd = this.jobRepository.findByJobIdIn(jobIds);

        for (Job job : jobsToAdd) {
            if (currentSavedJobs.stream().noneMatch(j -> j.getJobId() == job.getJobId())) {
                currentSavedJobs.add(job);
            }
        }

        currentUser.setSavedJobs(currentSavedJobs);
        this.userRepository.save(currentUser);

        return this.convertToUserResponse(currentUser);
    }

    @Override
    public UserResponse handleUnsaveJobsForCurrentUser(List<Long> jobIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return null;
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null) {
            return null;
        }

        List<Job> currentSavedJobs = currentUser.getSavedJobs() != null
                ? new ArrayList<>(currentUser.getSavedJobs())
                : new ArrayList<>();

        currentSavedJobs.removeIf(job -> jobIds.contains(job.getJobId()));

        currentUser.setSavedJobs(currentSavedJobs);
        this.userRepository.save(currentUser);

        return this.convertToUserResponse(currentUser);
    }

    /*     ========================= ========================= =========================  */

    /*     ========================= Saved Blogs Related Methods =========================  */
    @Override
    public ResultPaginationResponse handleGetCurrentUserSavedBlogs(Specification<Blog> spec, Pageable pageable) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        // Create specification to filter blogs saved by user
        Specification<Blog> userSavedSpec = (root, query, cb) ->
                cb.isMember(currentUser, root.get("users"));

        // Combine with spec from request
        Specification<Blog> finalSpec = spec == null ? userSavedSpec : spec.and(userSavedSpec);

        Page<Blog> page = this.blogRepository.findAll(finalSpec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<BlogResponse> blogResponses = page.getContent().stream()
                .map(blogService::convertToBlogResponse)
                .collect(Collectors.toList());

        return new ResultPaginationResponse(meta, blogResponses);
    }

    @Override
    public List<Map<String, Object>> handleCheckBlogsSaved(List<Long> blogIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return new ArrayList<>();
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        List<Long> savedBlogIds = currentUser.getSavedBlogs() != null
                ? currentUser.getSavedBlogs().stream().map(Blog::getBlogId).toList()
                : new ArrayList<>();

        return blogIds.stream()
                .map(blogId -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("blogId", blogId);
                    result.put("result", savedBlogIds.contains(blogId));
                    return result;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse handleSaveBlogsForCurrentUser(List<Long> blogIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return null;
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null) {
            return null;
        }

        List<Blog> blogsToAdd = this.blogRepository.findByBlogIdIn((blogIds));

        if (blogsToAdd.isEmpty()) {
            return this.convertToUserResponse(currentUser);
        }

        List<Blog> currentSavedBlogs = currentUser.getSavedBlogs() != null
                ? new ArrayList<>(currentUser.getSavedBlogs())
                : new ArrayList<>();


        for (Blog blog : blogsToAdd) {
            if (currentSavedBlogs.stream().noneMatch(b -> b.getBlogId() == blog.getBlogId())) {
                currentSavedBlogs.add(blog);
            }
        }

        currentUser.setSavedBlogs(currentSavedBlogs);
        this.userRepository.save(currentUser);

        return this.convertToUserResponse(currentUser);
    }

    @Override
    public UserResponse handleUnsaveBlogsForCurrentUser(List<Long> blogIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return null;
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null) {
            return null;
        }

        List<Blog> currentSavedBlogs = currentUser.getSavedBlogs() != null
                ? new ArrayList<>(currentUser.getSavedBlogs())
                : new ArrayList<>();

        currentSavedBlogs.removeIf(blog -> blogIds.contains(blog.getBlogId()));

        currentUser.setSavedBlogs(currentSavedBlogs);
        this.userRepository.save(currentUser);

        return this.convertToUserResponse(currentUser);
    }

    /*     ========================= ========================= =========================  */


//    // functions for notifications feature
//    @Override
//    public ResultPaginationResponse handleGetCurrentUserNotifications(Pageable pageable) {
//        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
//                SecurityUtil.getCurrentUserLogin().get() : "";
//
//        if (currentEmail.isEmpty()) {
//            return new ResultPaginationResponse(
//                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
//                    new ArrayList<>()
//            );
//        }
//
//        User currentUser = this.handleGetUserByEmail(currentEmail);
//        if (currentUser == null) {
//            return new ResultPaginationResponse(
//                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
//                    new ArrayList<>()
//            );
//        }
//
//        Page<Notification> page = this.notificationRepository
//                .findByRecipient_UserId(currentUser.getUserId(), pageable);
//
//        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
//        meta.setPage(pageable.getPageNumber() + 1);
//        meta.setPageSize(pageable.getPageSize());
//        meta.setPages(page.getTotalPages());
//        meta.setTotal(page.getTotalElements());
//
//        return new ResultPaginationResponse(meta, page.getContent());
//    }
//
//    @Override
//    public List<Notification> handleGetLatestNotifications() {
//        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
//                SecurityUtil.getCurrentUserLogin().get() : "";
//
//        if (currentEmail.isEmpty()) {
//            return new ArrayList<>();
//        }
//
//        User currentUser = this.handleGetUserByEmail(currentEmail);
//        if (currentUser == null) {
//            return new ArrayList<>();
//        }
//
//        PageRequest pageRequest = PageRequest.of(
//                0,
//                10,
//                Sort.by(Sort.Direction.DESC, "createdAt")
//        );
//
//        return this.notificationRepository
//                .findByRecipient_UserId(currentUser.getUserId(), pageRequest)
//                .getContent();
//    }
//
//    @Override
//    public void handleMarkNotificationsAsSeen(List<Long> notificationIds) {
//        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
//                SecurityUtil.getCurrentUserLogin().get() : "";
//
//        if (currentEmail.isEmpty()) {
//            return;
//        }
//
//        User currentUser = this.handleGetUserByEmail(currentEmail);
//        if (currentUser == null) {
//            return;
//        }
//
//        List<Notification> notifications = this.notificationRepository
//                .findByNotificationIdInAndRecipient_UserId(notificationIds, currentUser.getUserId());
//
//        notifications.forEach(notification -> notification.setSeen(true));
//        this.notificationRepository.saveAll(notifications);
//    }

    /*     ========================= Followed Companies Related Endpoints =========================  */
    @Override
    public List<Company> handleGetCurrentUserFollowedCompanies() {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return new ArrayList<>();
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null || currentUser.getFollowedCompanies() == null) {
            return new ArrayList<>();
        }

        return currentUser.getFollowedCompanies();
    }

    @Override
    public List<Map<String, Object>> handleCheckCompaniesFollowed(List<Long> companyIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return new ArrayList<>();
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        List<Long> followedIds = currentUser.getFollowedCompanies() != null
                ? currentUser.getFollowedCompanies().stream().map(Company::getAccountId).toList()
                : new ArrayList<>();

        return companyIds.stream()
                .map(companyId -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("companyId", companyId);
                    result.put("result", followedIds.contains(companyId));
                    return result;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean handleFollowCompanies(List<Long> companyIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return false;
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null) {
            return false;
        }

        int result = this.userRepository.followCompanies(currentUser.getAccountId(), companyIds);

        return result > 0;
    }

    @Override
    @Transactional
    public boolean handleUnfollowCompanies(List<Long> companyIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return false;
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        if (currentUser == null) {
            return false;
        }

        int result = this.userRepository.unfollowCompanies(currentUser.getAccountId(), companyIds);

        return result > 0;
    }
    /*     ========================= ========================= =========================  */

    /*     ========================= Reviewed Companies Related Endpoints =========================  */
    @Override
    public List<Map<String, Object>> handleCheckReviewedCompanies(List<Long> companyIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return new ArrayList<>();
        }

        User currentUser = this.handleGetUserByEmail(currentEmail);
        List<Long> reviewedIds = Optional.ofNullable(currentUser.getReviews())
                .orElse(new ArrayList<>())
                .stream()
                .map(review -> review.getCompany().getAccountId())
                .toList();

        return companyIds.stream()
                .map(companyId -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("companyId", companyId);
                    result.put("result", reviewedIds.contains(companyId));
                    return result;
                })
                .collect(Collectors.toList());
    }
    /*     ========================= ========================= =========================  */

    /*     ========================= Interview Related Endpoints =========================  */
    @Override
    public ResultPaginationResponse handleGetCurrentUserInterviews(Specification<Interview> spec, Pageable pageable) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        Account currentAccount = this.accountRepository.findByEmail(currentEmail).orElse(null);
        if (currentAccount == null) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        Role role = currentAccount.getRole();
        Specification<Interview> userInterviewSpec;
        switch (role.getName()) {
            case "APPLICANT" -> userInterviewSpec = (root, query, cb) ->
                    cb.and(
                        cb.equal(root.get("application").get("applicant").get("accountId"), currentAccount.getAccountId()),
                        cb.isNull(root.get("deletedAt"))
                    );
            case "RECRUITER" -> userInterviewSpec = (root, query, cb) ->
                    cb.and(
                        cb.equal(root.get("interviewer").get("accountId"), currentAccount.getAccountId()),
                        cb.isNull(root.get("deletedAt"))
                    );
            case "COMPANY" -> userInterviewSpec = (root, query, cb) -> {
                    Join<Object, Object> recruiterJoin = root.join("interviewer");
                    Join<Object, Object> companyJoin = recruiterJoin.join("company");
                    return cb.and(
                        cb.equal(companyJoin.get("accountId"), currentAccount.getAccountId()),
                        cb.isNull(root.get("deletedAt"))
                    );
            };
            default -> userInterviewSpec = (root, query, cb) ->
                    cb.isNull(root.get("deletedAt"));
        }

        Specification<Interview> finalSpec = spec == null ? userInterviewSpec : spec.and(userInterviewSpec);
        Page<Interview> page = this.interviewRepository.findAll(finalSpec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<InterviewResponse> interviewResponses = page.getContent().stream()
                .map(interviewService::handleConvertToInterviewResponse)
                .toList();

        return new ResultPaginationResponse(meta, interviewResponses);
    }
    /*     ========================= ========================= =========================  */

//    @Override
//    public List<UserEnabledResponse> handleActivateUsers(List<Long> userIds) {
//        return this.setUsersEnabled(userIds, true);
//    }
//
//    @Override
//    public List<UserEnabledResponse> handleDeactivateUsers(List<Long> userIds) {
//        return this.setUsersEnabled(userIds, false);
//    }
//
//    private List<UserEnabledResponse> setUsersEnabled(List<Long> userIds, boolean enabled) {
//        if (userIds == null || userIds.isEmpty()) {
//            return new ArrayList<>();
//        }
//
//        List<User> users = this.userRepository.findAllById(userIds);
//        if (users.isEmpty()) {
//            return new ArrayList<>();
//        }
//
//        users.forEach(u -> {
//            u.setEnabled(enabled);
//            this.emailNotificationService.handleSendUserEnabledEmail(
//                    u.getContact().getEmail(), u.getUsername(), enabled
//            );
//        });
//        this.userRepository.saveAll(users);
//
//        return users.stream()
//                .map(u -> new UserEnabledResponse(u.getUserId(), u.isEnabled()))
//                .collect(Collectors.toList());
//    }
//
    @Override
    public UserResponse convertToUserResponse(User user) {
        UserResponse userResponse = new UserResponse();

        userResponse.setAccountId(user.getAccountId());
        userResponse.setUsername(user.getUsername());
        userResponse.setEmail(user.getEmail());
        userResponse.setPhone(user.getPhone());
        userResponse.setAddresses(user.getAddresses());
        userResponse.setFullName(user.getFullName());
        userResponse.setAvatar(user.getAvatar());
        userResponse.setGender(user.getGender());
        userResponse.setDob(user.getDob());
        userResponse.setEnabled(user.isEnabled());
        userResponse.setCoverPhoto(user.getCoverPhoto());
        userResponse.setHeadline(user.getHeadline());
        userResponse.setBio(user.getBio());
        userResponse.setCreatedAt(user.getCreatedAt());
        userResponse.setUpdatedAt(user.getUpdatedAt());

        if (user.getRole() != null) {
            UserResponse.RoleUser roleUser = new UserResponse.RoleUser();
            roleUser.setRoleId(user.getRole().getRoleId());
            roleUser.setName(user.getRole().getName());

            userResponse.setRole(roleUser);
        }

        return userResponse;
    }
}
