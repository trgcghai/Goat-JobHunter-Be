package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.blog.BlogResponse;
import iuh.fit.goat.dto.response.company.CompanyResponse;
import iuh.fit.goat.dto.response.interview.InterviewResponse;
import iuh.fit.goat.dto.request.user.ResetPasswordRequest;
import iuh.fit.goat.dto.response.auth.LoginResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.job.JobResponse;
import iuh.fit.goat.dto.response.user.UserEnabledResponse;
import iuh.fit.goat.dto.response.user.UserResponse;
import iuh.fit.goat.dto.response.user.UserVisibilityResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.enumeration.Visibility;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.service.*;
import iuh.fit.goat.util.BasicUtil;
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
    private final RedisService redisService;
    private final EmailNotificationService emailNotificationService;
    private final JobService jobService;
    private final CompanyService companyService;

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final NotificationRepository notificationRepository;
    private final BlogRepository blogRepository;
    private final InterviewRepository interviewRepository;

    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;
//    private final RoleRepository roleRepository;

    @Value("${minhdat.jwt.refresh-token-validity-in-seconds}")
    private long jwtRefreshToken;
    @Value("${minhdat.verify-code-validity-in-seconds}")
    private long validityInSeconds;

    @Override
    public Account handleGetAccountByEmail(String email) {
        return this.accountRepository.findByEmailWithRole(email).orElse(null);
    }

    @Override
    public boolean handleExistsByEmail(String email) {
        return this.userRepository.existsByEmail(email);
    }

    @Override
    public User handleGetUserById(long id) {
        return this.userRepository.findByAccountIdAndDeletedAtIsNull(id).orElse(null);
    }

    @Override
    public ResultPaginationResponse handleGetAllUsers(Specification<User> spec, Pageable pageable) {
        Page<User> page = this.userRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<UserResponse> userResponses = page.getContent().stream()
                .map(this::convertToUserResponse)
                .toList();

        return new ResultPaginationResponse(meta, userResponses);
    }

    @Override
    public ResultPaginationResponse handleSearchUsers(String searchTerm, Pageable pageable) throws InvalidException {

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        // Get current logged-in user email
        String currentUserEmail = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new InvalidException("User not authenticated"));
        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentUserEmail)
            .orElseThrow(() -> new InvalidException("Current account doesn't exist"));

        // Fetch filtered users using repository method
        Page<User> pageUser = this.userRepository.searchUsers(
            searchTerm,
            currentUserEmail,
            currentAccount.getAccountId(),
            pageable
        );

        // Convert to response
        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(pageUser.getTotalPages());
        meta.setTotal(pageUser.getTotalElements());

        List<UserResponse> userResponses = pageUser.getContent().stream()
                .map(this::convertToUserResponse)
                .toList();

        return new ResultPaginationResponse(meta, userResponses);
    }

    @Override
    public boolean handleCheckCurrentPassword(String currentPassword) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();
        if (currentEmail.isEmpty()) {
            return false;
        }

        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        if (currentAccount == null) {
            return false;
        }

        return this.passwordEncoder.matches(currentPassword, currentAccount.getPassword());
    }

    @Override
    public Map<String, Object> handleUpdatePassword(String newPassword, String refreshToken) throws InvalidException {
        String currentEmail = SecurityUtil.getCurrentUserEmail();
        if (currentEmail.isEmpty()) {
            throw new InvalidException("User not authenticated");
        }

        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail)
                .orElseThrow(() -> new InvalidException("User not found"));
        String hashedPassword = this.passwordEncoder.encode(newPassword);
        currentAccount.setPassword(hashedPassword);
        Account res = this.accountRepository.save(currentAccount);

        LoginResponse loginResponse = new LoginResponse();

        // Thông tin chung của Account
        loginResponse.setAccountId(res.getAccountId());
        loginResponse.setEmail(res.getEmail());
        loginResponse.setUsername(Objects.requireNonNullElse(res.getUsername(), ""));
        loginResponse.setAvatar(Objects.requireNonNullElse(res.getAvatar(), ""));
        loginResponse.setEnabled(res.isEnabled());
        loginResponse.setAddresses(Objects.requireNonNullElse(res.getAddresses(), new ArrayList<>()));

        if (res.getRole() != null) {
            loginResponse.setRole(
                    new LoginResponse.RoleAccount(res.getRole().getRoleId(), res.getRole().getName())
            );
        }

        // Thông tin riêng của User
        if (res instanceof User user) {
            loginResponse.setPhone(user.getPhone());
            loginResponse.setDob(user.getDob());
            loginResponse.setAddresses(user.getAddresses());
            loginResponse.setGender(user.getGender());
            loginResponse.setFullName(Objects.requireNonNullElse(user.getFullName(), ""));
            loginResponse.setType(user instanceof Applicant ? iuh.fit.goat.common.Role.APPLICANT.getValue() : iuh.fit.goat.common.Role.RECRUITER.getValue());

            // Nếu như là Recruiter thì mới có company
            if (user instanceof Recruiter recruiter) {
                LoginResponse.UserCompany userCompany = new LoginResponse.UserCompany(
                        recruiter.getCompany().getAccountId(),
                        recruiter.getCompany().getName()
                );
                loginResponse.setCompany(userCompany);
            }
        }
        // Thông tin riêng của Company
        else if (res instanceof Company company) {
            loginResponse.setPhone(company.getPhone());
            loginResponse.setAddresses(company.getAddresses());
            loginResponse.setName(Objects.requireNonNullElse(company.getName(), ""));
            loginResponse.setType(iuh.fit.goat.common.Role.COMPANY.getValue());
            loginResponse.setDescription(company.getDescription());
            loginResponse.setLogo(company.getLogo());
            loginResponse.setCoverPhoto(company.getCoverPhoto());
            loginResponse.setWebsite(company.getWebsite());
            loginResponse.setSize(company.getSize());
            loginResponse.setVerified(company.isVerified());
            loginResponse.setCountry(company.getCountry());
            loginResponse.setIndustry(company.getIndustry());
            loginResponse.setWorkingDays(company.getWorkingDays());
            loginResponse.setOvertimePolicy(company.getOvertimePolicy());
        }

        String newAccessToken = this.securityUtil.createAccessToken(currentEmail, loginResponse);
        String newRefreshToken = this.securityUtil.createRefreshToken(currentEmail, loginResponse);
        this.redisService.replaceKey(
                "refresh:" + refreshToken,
                "refresh:" + newRefreshToken,
                currentEmail, jwtRefreshToken, TimeUnit.SECONDS

        );

        Map<String, Object> response = new HashMap<>();
        response.put("loginResponse", loginResponse);
        response.put("refreshToken", newRefreshToken);
        response.put("accessToken", newAccessToken);

        return response;
    }


    @Override
    public void handleResetPassword(ResetPasswordRequest resetPasswordRequest) throws InvalidException {
        Account account = this.handleGetAccountByEmail(resetPasswordRequest.getEmail());
        if (account != null) {
            String hashedPassword = this.passwordEncoder.encode(resetPasswordRequest.getNewPassword());
            account.setPassword(hashedPassword);
            account.setEnabled(false);
            this.accountRepository.save(account);

            String verificationCode = BasicUtil.generateVerificationCode();
            this.redisService.saveWithTTL(
                    account.getEmail(),
                    verificationCode,
                    validityInSeconds,
                    TimeUnit.SECONDS
            );
            this.emailNotificationService.handleSendVerificationEmail(account.getEmail(), verificationCode);
        } else {
            throw new InvalidException("User not found");
        }
    }


    /*     ========================= Saved Job Related Methods =========================  */

    @Override
    public ResultPaginationResponse handleGetCurrentAccountSavedJobs(Pageable pageable) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        if (currentAccount == null || currentAccount.getSavedJobs() == null) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        List<Job> savedJobs = currentAccount.getSavedJobs();
        List<JobResponse> jobResponses = savedJobs.stream().map(this.jobService::convertToJobResponse).collect(Collectors.toList());
        int total = jobResponses.size();
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int start = pageNumber * pageSize;
        List<JobResponse> content;

        if (start >= total || pageSize <= 0) {
            content = new ArrayList<>();
        } else {
            int end = Math.min(start + pageSize, total);
            content = jobResponses.subList(start, end);
        }

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageNumber + 1);
        meta.setPageSize(pageSize);
        int pages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
        meta.setPages(pages);
        meta.setTotal(total);

        return new ResultPaginationResponse(meta, content);
    }

    @Override
    public List<Map<String, Object>> handleCheckJobsSaved(List<Long> jobIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return new ArrayList<>();
        }

        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        assert currentAccount != null;
        List<Long> savedJobIds = currentAccount.getSavedJobs() != null
                ? currentAccount.getSavedJobs().stream().map(Job::getJobId).toList()
                : new ArrayList<>();

        return jobIds.stream()
                .map(jobId -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("jobId", jobId);
                    result.put("result", savedJobIds.contains(jobId));
                    return result;
                })
                .toList();
    }

    @Override
    public Object handleSaveJobsForCurrentAccount(List<Long> jobIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return null;
        }

        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        if (currentAccount == null) {
            return null;
        }

        List<Job> currentSavedJobs = currentAccount.getSavedJobs() != null
                ? new ArrayList<>(currentAccount.getSavedJobs())
                : new ArrayList<>();

        List<Job> jobsToAdd = this.jobRepository.findByJobIdIn(jobIds);

        for (Job job : jobsToAdd) {
            if (currentSavedJobs.stream().noneMatch(j -> j.getJobId() == job.getJobId())) {
                currentSavedJobs.add(job);
            }
        }

        currentAccount.setSavedJobs(currentSavedJobs);
        this.accountRepository.save(currentAccount);

        return currentAccount instanceof Company
                ? this.companyService.convertToCompanyResponse((Company) currentAccount)
                : this.convertToUserResponse((User) currentAccount);
    }

    @Override
    public Object handleUnsaveJobsForCurrentAccount(List<Long> jobIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return null;
        }

        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        if (currentAccount == null) {
            return null;
        }

        List<Job> currentSavedJobs = currentAccount.getSavedJobs() != null
                ? new ArrayList<>(currentAccount.getSavedJobs())
                : new ArrayList<>();

        currentSavedJobs.removeIf(job -> jobIds.contains(job.getJobId()));

        currentAccount.setSavedJobs(currentSavedJobs);
        this.accountRepository.save(currentAccount);

        return currentAccount instanceof Company
                ? this.companyService.convertToCompanyResponse((Company) currentAccount)
                : this.convertToUserResponse((User) currentAccount);
    }

    /*     ========================= ========================= =========================  */

    /*     ========================= Saved Blogs Related Methods =========================  */
    @Override
    public ResultPaginationResponse handleGetCurrentAccountSavedBlogs(Specification<Blog> spec, Pageable pageable) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        if (currentAccount == null) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        // Create specification to filter blogs saved by user
        Specification<Blog> accountSavedSpec = (root, query, cb) ->
                cb.isMember(currentAccount, root.get("accounts"));

        // Combine with spec from request
        Specification<Blog> finalSpec = spec == null ? accountSavedSpec : spec.and(accountSavedSpec);

        Page<Blog> page = this.blogRepository.findAll(finalSpec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<BlogResponse> blogResponses = page.getContent().stream()
                .map(blogService::convertToBlogResponse)
                .toList();

        return new ResultPaginationResponse(meta, blogResponses);
    }

    @Override
    public List<Map<String, Object>> handleCheckBlogsSaved(List<Long> blogIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return new ArrayList<>();
        }

        Account currentUser = this.accountRepository.findByEmailWithRole(currentEmail).orElse(null);
        if(currentUser == null) {
            return new ArrayList<>();
        }

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
                .toList();
    }

    @Override
    @Transactional
    public Object handleSaveBlogsForCurrentAccount(List<Long> blogIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return null;
        }

        Account currentAccount = this.accountRepository.findByEmailWithRole(currentEmail).orElse(null);
        if (currentAccount == null) {
            return null;
        }

        List<Blog> blogsToAdd = this.blogRepository.findByBlogIdIn((blogIds));

        if (blogsToAdd.isEmpty()) {
            return currentAccount instanceof Company
                    ? this.companyService.convertToCompanyResponse((Company) currentAccount)
                    : this.convertToUserResponse((User) currentAccount);
        }

        List<Blog> currentSavedBlogs = currentAccount.getSavedBlogs() != null
                ? new ArrayList<>(currentAccount.getSavedBlogs())
                : new ArrayList<>();


        for (Blog blog : blogsToAdd) {
            if (currentSavedBlogs.stream().noneMatch(b -> b.getBlogId() == blog.getBlogId())) {
                currentSavedBlogs.add(blog);
            }
        }

        currentAccount.setSavedBlogs(currentSavedBlogs);
        this.accountRepository.save(currentAccount);

        return currentAccount instanceof Company
                ? this.companyService.convertToCompanyResponse((Company) currentAccount)
                : this.convertToUserResponse((User) currentAccount);
    }

    @Override
    public Object handleUnsaveBlogsForCurrentAccount(List<Long> blogIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return null;
        }

        Account currentAccount = this.accountRepository.findByEmailWithRole(currentEmail).orElse(null);
        if (currentAccount == null) {
            return null;
        }

        List<Blog> currentSavedBlogs = currentAccount.getSavedBlogs() != null
                ? new ArrayList<>(currentAccount.getSavedBlogs())
                : new ArrayList<>();

        currentSavedBlogs.removeIf(blog -> blogIds.contains(blog.getBlogId()));

        currentAccount.setSavedBlogs(currentSavedBlogs);
        this.accountRepository.save(currentAccount);

        return currentAccount instanceof Company
                ? this.companyService.convertToCompanyResponse((Company) currentAccount)
                : this.convertToUserResponse((User) currentAccount);
    }

    /*     ========================= ========================= =========================  */


    // functions for notifications feature
    @Override
    public ResultPaginationResponse handleGetCurrentUserNotifications(Pageable pageable) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if (currentEmail.isEmpty()) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        if (currentAccount == null) {
            return new ResultPaginationResponse(
                    new ResultPaginationResponse.Meta(0, 0, 0, 0L),
                    new ArrayList<>()
            );
        }

        Page<Notification> page = this.notificationRepository
                .findByRecipient_AccountId(currentAccount.getAccountId(), pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        return new ResultPaginationResponse(meta, page.getContent());
    }

    @Override
    public List<Notification> handleGetLatestNotifications() {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if (currentEmail.isEmpty()) {
            return new ArrayList<>();
        }

        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        if (currentAccount == null) {
            return new ArrayList<>();
        }

        PageRequest pageRequest = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return this.notificationRepository
                .findByRecipient_AccountId(currentAccount.getAccountId(), pageRequest)
                .getContent();
    }

    @Override
    public void handleMarkNotificationsAsSeen(List<Long> notificationIds) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent() ?
                SecurityUtil.getCurrentUserLogin().get() : "";

        if (currentEmail.isEmpty()) {
            return;
        }

        Account currentAccount = this.handleGetAccountByEmail(currentEmail);
        if (currentAccount == null) {
            return;
        }

        List<Notification> notifications = this.notificationRepository
                .findByNotificationIdInAndRecipient_AccountId(notificationIds, currentAccount.getAccountId());

        notifications.forEach(notification -> notification.setSeen(true));
        this.notificationRepository.saveAll(notifications);
    }

    /*     ========================= Followed Companies Related Endpoints =========================  */
    @Override
    public List<CompanyResponse> handleGetCurrentAccountFollowedCompanies() {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return new ArrayList<>();
        }

        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        if (currentAccount == null || currentAccount.getFollowedCompanies() == null) {
            return new ArrayList<>();
        }

        return currentAccount.getFollowedCompanies().stream().map(this.companyService::convertToCompanyResponse).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> handleCheckCompaniesFollowed(List<Long> companyIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return new ArrayList<>();
        }

        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        assert currentAccount != null;
        List<Long> followedIds = currentAccount.getFollowedCompanies() != null
                ? currentAccount.getFollowedCompanies().stream().map(Company::getAccountId).toList()
                : new ArrayList<>();

        return companyIds.stream()
                .map(companyId -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("companyId", companyId);
                    result.put("result", followedIds.contains(companyId));
                    return result;
                })
                .toList();
    }

    @Override
    @Transactional
    public boolean handleFollowCompanies(List<Long> companyIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return false;
        }

        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        if (currentAccount == null) {
            return false;
        }

        int result = this.accountRepository.followCompanies(currentAccount.getAccountId(), companyIds);

        return result > 0;
    }

    @Override
    @Transactional
    public boolean handleUnfollowCompanies(List<Long> companyIds) {
        String currentEmail = SecurityUtil.getCurrentUserEmail();

        if (currentEmail.isEmpty()) {
            return false;
        }

        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        if (currentAccount == null) {
            return false;
        }

        int result = this.accountRepository.unfollowCompanies(currentAccount.getAccountId(), companyIds);

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

        Account currentAccount = this.handleGetAccountByEmail(currentEmail);
        if(currentAccount instanceof Company) {
            return new ArrayList<>();
        }
        List<Long> reviewedIds = Optional.ofNullable(((User)currentAccount).getReviews())
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
                .toList();
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

        Account currentAccount = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
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

    @Override
    public List<UserEnabledResponse> handleActivateUsers(List<Long> userIds) {
        return this.setUsersEnabled(userIds, true);
    }

    @Override
    public List<UserEnabledResponse> handleDeactivateUsers(List<Long> userIds) {
        return this.setUsersEnabled(userIds, false);
    }

    @Override
    @Transactional
    public UserVisibilityResponse handleUpdateMyVisibility(Visibility visibility) throws InvalidException {
        String currentEmail = SecurityUtil.getCurrentUserEmail();
        if (currentEmail == null || currentEmail.isBlank()) {
            throw new InvalidException("User not authenticated");
        }

        Account account = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail)
                .orElseThrow(() -> new InvalidException("Account not found"));

        account.setVisibility(visibility);
        Account savedAccount = this.accountRepository.save(account);

        return new UserVisibilityResponse(savedAccount.getAccountId(), savedAccount.getVisibility());
    }

    @Override
    @Transactional
    public List<UserVisibilityResponse> handleUpdateUsersVisibility(List<Long> accountIds, Visibility visibility)
            throws InvalidException {
        if (accountIds == null || accountIds.isEmpty()) {
            throw new InvalidException("Account IDs list cannot be empty");
        }

        List<Long> uniqueAccountIds = accountIds.stream().distinct().toList();
        List<Account> accounts = this.accountRepository.findAllByAccountIdInAndDeletedAtIsNull(uniqueAccountIds);

        if (accounts.size() != uniqueAccountIds.size()) {
            Set<Long> foundIds = accounts.stream()
                    .map(Account::getAccountId)
                    .collect(Collectors.toSet());

            List<Long> missingIds = uniqueAccountIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            throw new InvalidException("Accounts not found: " + missingIds);
        }

        accounts.forEach(account -> account.setVisibility(visibility));
        List<Account> savedAccounts = this.accountRepository.saveAll(accounts);

        return savedAccounts.stream()
                .map(account -> new UserVisibilityResponse(account.getAccountId(), account.getVisibility()))
                .toList();
    }

    private List<UserEnabledResponse> setUsersEnabled(List<Long> userIds, boolean enabled) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<User> users = this.userRepository.findAllById(userIds);
        if (users.isEmpty()) {
            return new ArrayList<>();
        }

        users.forEach(u -> {
            u.setEnabled(enabled);
            this.emailNotificationService.handleSendUserEnabledEmail(
                    u.getEmail(), u.getUsername(), enabled
            );
        });
        this.userRepository.saveAll(users);

        return users.stream()
                .map(u -> new UserEnabledResponse(u.getAccountId(), u.isEnabled()))
                .toList();
    }

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
        userResponse.setVisibility(user.getVisibility());
        userResponse.setCoverPhoto(user.getCoverPhoto());
        userResponse.setHeadline(user.getHeadline());
        userResponse.setBio(user.getBio());
        userResponse.setCreatedAt(user.getCreatedAt());
        userResponse.setUpdatedAt(user.getUpdatedAt());

        if (user.getRole() != null) {
            UserResponse.RoleAccount roleAccount = new UserResponse.RoleAccount();
            roleAccount.setRoleId(user.getRole().getRoleId());
            roleAccount.setName(user.getRole().getName());

            userResponse.setRole(roleAccount);
        }

        return userResponse;
    }
}
