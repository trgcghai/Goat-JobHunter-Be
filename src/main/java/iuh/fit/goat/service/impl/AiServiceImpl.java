package iuh.fit.goat.service.impl;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import iuh.fit.goat.common.MessageRole;
import iuh.fit.goat.common.Role;
import iuh.fit.goat.dto.request.ai.ChatRequest;
import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.service.*;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {
    private final Client client;

    private final UserService userService;
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final CacheService cacheService;;

    private final JobRepository jobRepository;
    private final ApplicantRepository applicantRepository;
    private final RecruiterRepository recruiterRepository;
    private final ApplicationRepository applicationRepository;
    private final SubscriberRepository subscriberRepository;
    private final BlogRepository blogRepository;
    private final CareerRepository careerRepository;
    private final SkillRepository skillRepository;

    @Value("${google.api.model}")
    private String MODEL;
    @Value("${goat.fe.url}")
    private String FE;

    private final String CACHE_NAME = "aiChat";
    private final Long TTL = 86400L;

    @Override
    @Transactional(readOnly = true)
    public String chatWithAi(ChatRequest request) {
        String currentUserEmail = SecurityUtil.getCurrentUserLogin().orElse(null);
        User currentUser = null;
        Role currentUserRole = null;

        if (currentUserEmail != null) {
            currentUser = this.userService.handleGetUserByEmail(currentUserEmail);
            if (currentUser != null && currentUser.getRole() != null) {
                currentUserRole = getRoleFromUser(currentUser);
            }
        }

        String systemPrompt = buildSystemPrompt(currentUser, currentUserRole);
        String contextData = buildSmartContext(currentUser, currentUserRole);
        String conversationHistory = getOptimizedConversationHistory(request.getConversationId(), currentUser);

        if (request.getConversationId() != null && currentUser != null) {
            this.messageService.handleCreateMessage(new MessageCreateRequest(
                    request.getConversationId(), MessageRole.USER, request.getMessage()
            ));
        }

        String aiResponse = callAiApi(systemPrompt, contextData, conversationHistory, request.getMessage());

        if (request.getConversationId() != null && currentUser != null) {
            this.messageService.handleCreateMessage(new MessageCreateRequest(
                    request.getConversationId(), MessageRole.AI, aiResponse
            ));
            this.conversationService.handleUpdateTitleIfFirstAiMessage(
                    request.getConversationId(), aiResponse
            );
        }

        return aiResponse;
    }

    // Lấy vai trò của current user
    private Role getRoleFromUser(User user) {
        String roleName = user.getRole().getName();
        if (roleName.equals(Role.ADMIN.getValue())) return Role.ADMIN;
        if (roleName.equals(Role.RECRUITER.getValue())) return Role.RECRUITER;
        if (roleName.equals(Role.APPLICANT.getValue())) return Role.APPLICANT;
        return null;
    }
    // Lấy vai trò của current user


    // Build prompt cho user phù hợp với từng vai trò
    private String buildSystemPrompt(User currentUser, Role currentUserRole) {
        String basePrompt = """
            Bạn là trợ lý AI thông minh của hệ thống "Goat Tìm Kiếm Việc Làm".
            Current date: %s.
            
            Instructions:
            1. Trả lời bất kỳ câu hỏi nào: về GOAT hoặc kiến thức tổng quát.
            2. Ưu tiên dữ liệu nội bộ khi câu hỏi liên quan GOAT.
            3. Trả lời Tiếng Việt, thân thiện, ngắn gọn.
            4. Chỉ trả lời dữ liệu người dùng có quyền xem.
            5. Link job và blog dạng: [title](url) - KHÔNG ghi ID.
            6. Format lương về VNĐ (K = nghìn).
            7. Sử dụng context và lịch sử hội thoại để trả lời chính xác.
            8. Khi đề cập blog, luôn gửi link clickable và thông tin tác giả.
            9. Khi đề cập career/ngành nghề, liệt kê số lượng job liên quan.
            """.formatted(new Date());

        if (currentUserRole == Role.ADMIN) {
            return basePrompt + "\nBạn đang hỗ trợ ADMIN - có quyền truy cập toàn bộ dữ liệu.";
        } else if (currentUserRole == Role.RECRUITER) {
            return basePrompt + String.format(
                    "\nBạn đang hỗ trợ Recruiter %s - chỉ xem dữ liệu liên quan họ.",
                    currentUser.getFullName()
            );
        } else if (currentUserRole == Role.APPLICANT) {
            return basePrompt + String.format(
                    "\nBạn đang hỗ trợ Applicant %s - tư vấn job và application.",
                    currentUser.getFullName()
            );
        } else {
            return basePrompt + "\nBạn đang hỗ trợ GUEST - chỉ xem công việc công khai.";
        }
    }

    private String buildSmartContext(User currentUser, Role currentUserRole) {
        StringBuilder context = new StringBuilder("\n--- DỮ LIỆU NGỮ CẢNH ---\n");

        if (currentUserRole == Role.ADMIN) {
            context.append(buildAdminContext());
        } else if (currentUserRole == Role.RECRUITER) {
            context.append(buildRecruiterContext((Recruiter) currentUser));
        } else if (currentUserRole == Role.APPLICANT) {
            context.append(buildApplicantContext((Applicant) currentUser));
        } else {
            context.append(buildGuestContext());
        }

        return context.toString();
    }

    private String buildAdminContext() {
        StringBuilder sb = new StringBuilder();

        sb.append("[JOBS]\n").append(getTopJobsContext()).append("\n\n");
        sb.append("[APPLICANTS]\n").append(getTopApplicantsContext()).append("\n\n");
        sb.append("[RECRUITERS]\n").append(getTopRecruitersContext()).append("\n\n");
        sb.append("[APPLICATIONS]\n").append(getRecentApplicationsContext()).append("\n\n");
        sb.append("[SKILLS]\n").append(getTopSkillsContext()).append("\n\n");
        sb.append("[BLOGS]\n").append(getRecentBlogsContext()).append("\n\n");
        sb.append("[CAREERS]\n").append(getAllCareersContext()).append("\n\n");
        sb.append("[STATISTICS]\n").append(getSystemStatsContext()).append("\n\n");

        return sb.toString();
    }

    private String buildRecruiterContext(Recruiter recruiter) {
        StringBuilder sb = new StringBuilder();

        sb.append("[MY JOBS]\n").append(getJobsContextForRecruiter(recruiter)).append("\n\n");
        sb.append("[APPLICATIONS TO MY JOBS]\n").append(getApplicationsContextForRecruiter(recruiter)).append("\n\n");
        sb.append("[RELEVANT APPLICANTS]\n").append(getRelevantApplicantsForRecruiter(recruiter)).append("\n\n");
        sb.append("[SKILL TRENDS]\n").append(getTopSkillsContext()).append("\n\n");
        sb.append("[RECENT BLOGS]\n").append(getRecentBlogsContext()).append("\n\n");
        sb.append("[CAREERS]\n").append(getAllCareersContext()).append("\n\n");

        return sb.toString();
    }

    private String buildApplicantContext(Applicant applicant) {
        StringBuilder sb = new StringBuilder();

        sb.append("[MY APPLICATIONS]\n").append(getApplicationsContextForApplicant(applicant)).append("\n\n");
        sb.append("[RECOMMENDED JOBS]\n").append(getRecommendedJobsForApplicant(applicant)).append("\n\n");
        sb.append("[MY SUBSCRIPTIONS]\n").append(getSubscribersContextByApplicant(applicant)).append("\n\n");
        sb.append("[SKILL TRENDS]\n").append(getTopSkillsContext()).append("\n\n");
        sb.append("[RECOMMENDED BLOGS]\n").append(getRecentBlogsContext()).append("\n\n");
        sb.append("[CAREERS]\n").append(getAllCareersContext()).append("\n\n");

        return sb.toString();
    }

    private String buildGuestContext() {
        StringBuilder sb = new StringBuilder();

        sb.append("[AVAILABLE JOBS]\n").append(getTopJobsContext()).append("\n\n");
        sb.append("[TRENDING SKILLS]\n").append(getTopSkillsContext()).append("\n\n");
        sb.append("[RECENT BLOGS]\n").append(getRecentBlogsContext()).append("\n\n");
        sb.append("[CAREERS]\n").append(getAllCareersContext()).append("\n\n");
        sb.append("[JOB MARKET OVERVIEW]\n").append(getJobMarketOverview()).append("\n\n");

        return sb.toString();
    }
    // Build prompt cho user phù hợp với từng vai trò


    // Lấy dữ liệu lưu vào cache
    @Override
    public String getTopJobsContext() {
        return this.getOrSet(
                "allJobs",
                () -> {
                    List<Job> jobs = this.jobRepository.findAll();
                    if (jobs.isEmpty()) return "Không có công việc nào.";

                    return jobs.stream().map(
                            this::formatJobContext
                    ).collect(Collectors.joining("\n"));
                }
        );
    }

    @Override
    public String getTopApplicantsContext() {
        return this.getOrSet(
                "allApplicants",
                () -> {
                    List<Applicant> applicants = this.applicantRepository.findAll();
                    if (applicants.isEmpty()) return "Không có ứng viên.";

                    return applicants.stream().map(this::formatApplicantContext).collect(Collectors.joining("\n"));
                }
        );
    }

    @Override
    public String getTopRecruitersContext() {
        return this.getOrSet(
                "allRecruiters",
                () -> {
                    List<Recruiter> recruiters = this.recruiterRepository.findAll()
                            .stream()
                            .filter(r -> r.getUserId() != 1L)
                            .toList();
                    if (recruiters.isEmpty()) return "Không có recruiter.";

                    return recruiters.stream().map(this::formatRecruiterContext).collect(Collectors.joining("\n"));
                }
        );
    }

    @Override
    public String getRecentApplicationsContext() {
        return this.getOrSet(
                "allApplications",
                () -> {
                    List<Application> apps = this.applicationRepository.findAll();
                    if (apps.isEmpty()) return "Không có đơn ứng tuyển.";

                    return apps.stream().map(this::formatApplicationContext).collect(Collectors.joining("\n"));
                }
        );
    }

    @Override
    public String getTopSkillsContext() {
        return this.getOrSet(
                "allSkills",
                () -> {
                    List<Skill> skills = this.skillRepository.findAll();

                    Map<String, Long> skillJobCounts = skills.stream()
                            .collect(Collectors.toMap(
                                    Skill::getName,
                                    skill -> (long) skill.getJobs().size(),
                                    (a, b) -> a
                            ));

                    if (skillJobCounts.isEmpty()) return "Không có skill nào.";

                    return skills.stream()
                            .sorted((a, b) -> b.getJobs().size() - a.getJobs().size())
                            .map(s -> String.format(
                                    "- %s: %d jobs | Subscribers: %d",
                                    s.getName(),
                                    s.getJobs().size(),
                                    s.getSubscribers() != null ? s.getSubscribers().size() : 0
                            ))
                            .collect(Collectors.joining("\n"));
                }
        );
    }

    @Override
    public String getRecentBlogsContext() {
        return this.getOrSet(
                "allBlogs",
                () -> {
                    List<Blog> blogs = this.blogRepository.findAll();
                    if (blogs.isEmpty()) return "Không có blog nào.";

                    return blogs.stream().map(this::formatBlogContext).collect(Collectors.joining("\n"));
                }
        );
    }

    @Override
    public String getAllCareersContext() {
        return this.getOrSet(
                "allCareers",
                () -> {
                    List<Career> careers = this.careerRepository.findAll();
                    if (careers.isEmpty()) return "Không có career nào.";

                    return careers.stream()
                                    .map(career -> String.format(
                                            "- %s: %d jobs",
                                            career.getName(),
                                            career.getJobs() != null ? career.getJobs().size() : 0
                                    ))
                                    .collect(Collectors.joining("\n"));
                }
        );


    }

    @Override
    public String getSystemStatsContext() {
        return this.getOrSet(
                "systemStats",
                () -> String.format("""
                                    - Tổng Jobs: %d
                                    - Tổng Applicants: %d
                                    - Tổng Recruiters: %d
                                    - Tổng Applications: %d
                                    - Jobs Active: %d
                                    - Tổng Blogs: %d
                                    - Tổng Careers: %d
                                    - Tổng Skills: %d
                                    """,
                        this.jobRepository.count(),
                        this.applicantRepository.count(),
                        this.recruiterRepository.count() - 1,
                        this.applicationRepository.count(),
                        this.jobRepository.countByActive(true),
                        this.blogRepository.count(),
                        this.careerRepository.count(),
                        this.skillRepository.count()
                )
        );

    }

    @Override
    public String getJobMarketOverview() {
        return this.getOrSet(
                "jobOverview",
                () -> {
                    return String.format("""
                            - Tổng công việc đang tuyển: %d
                            - Mức lương trung bình: %.0fK VNĐ
                            - Vị trí nhiều nhất: %s
                            """,
                            this.jobRepository.countByActive(true),
                            this.jobRepository.findAll().stream()
                                    .mapToDouble(Job::getSalary)
                                    .average()
                                    .orElse(0) / 1000,
                            this.jobRepository.findAll().stream()
                                    .collect(Collectors.groupingBy(Job::getLocation, Collectors.counting()))
                                    .entrySet().stream()
                                    .max(Map.Entry.comparingByValue())
                                    .map(Map.Entry::getKey)
                                    .orElse("N/A")
                    );
                }
        );
    }
    // Lấy dữ liệu lưu vào cache


    // Format response
    private String formatJobContext(Job job) {
        return String.format(
                "- [%s](%s/jobs/%d) | %.0fK VNĐ | %s | %s | Skills: %s | Recruiter: %s | Qty: %d | Type: %s | Career: %s | Deadline: %s | Active: %s",
                job.getTitle(),
                FE,
                job.getJobId(),
                job.getSalary() / 1000,
                job.getLevel().getValue(),
                job.getLocation(),
                job.getSkills().stream()
                        .map(Skill::getName)
                        .limit(5)
                        .collect(Collectors.joining(", ")),
                job.getRecruiter() != null ? job.getRecruiter().getFullName() : "N/A",
                job.getQuantity(),
                job.getWorkingType().name(),
                job.getCareer() != null ? job.getCareer().getName() : "N/A",
                job.getEndDate() != null ? job.getEndDate().toString() : "N/A",
                job.isActive() ? "Yes" : "No"
        );
    }

    private String formatApplicantContext(Applicant a) {
        return String.format(
                "- %s | Level %s | %s | Education: %s | Gender: %s | DOB: %s | Phone: %s | Email: %s | Resume: %s | Applications: %d | Enabled: %s",
                a.getFullName(),
                a.getLevel() != null ? a.getLevel().getValue() : "N/A",
                a.isAvailableStatus() ? "Available" : "Busy",
                a.getEducation() != null ? a.getEducation().name() : "N/A",
                a.getGender() != null ? a.getGender().name() : "N/A",
                a.getDob() != null ? a.getDob().toString() : "N/A",
                a.getContact() != null ? a.getContact().getPhone() : "N/A",
                a.getContact() != null ? a.getContact().getEmail() : "N/A",
                a.getResumeUrl() != null ? a.getResumeUrl() : "N/A",
                a.getApplications() != null ? a.getApplications().size() : 0,
                a.isEnabled() ? "Yes" : "No"
        );
    }

    private String formatRecruiterContext(Recruiter r) {
        return String.format(
                "- %s | %s | Description: %s | Phone: %s | Email: %s | Gender: %s | Address: %s | Jobs: %d | Followers: %d | Enabled: %s",
                r.getFullName(),
                r.getWebsite() != null ? r.getWebsite() : "N/A",
                r.getDescription() != null ? shorten(r.getDescription(), 120) : "N/A",
                r.getContact() != null ? r.getContact().getPhone() : "N/A",
                r.getContact() != null ? r.getContact().getEmail() : "N/A",
                r.getGender() != null ? r.getGender().name() : "N/A",
                r.getAddress() != null ? r.getAddress() : "N/A",
                r.getJobs() != null ? r.getJobs().size() : 0,
                r.getUsers() != null ? r.getUsers().size() : 0,
                r.isEnabled() ? "Yes" : "No"
        );
    }

    private String formatApplicationContext(Application a) {
        Job job = a.getJob();
        Applicant applicant = a.getApplicant();

        return String.format(
                "- Job: %s | Applicant: %s | Status: %s"
                        + " | Email: %s | Resume: %s | Applied At: %s"
                        + " | Job Location: %s | Job Salary: %.0fK | Recruiter: %s"
                        + " | Applicant Level: %s | Applicant Gender: %s",

                job != null ? job.getTitle() : "N/A",
                applicant != null ? applicant.getFullName() : "N/A",
                a.getStatus() != null ? a.getStatus().getValue() : "N/A",

                a.getEmail() != null ? a.getEmail() : "N/A",
                a.getResumeUrl() != null ? a.getResumeUrl() : "N/A",
                a.getCreatedAt() != null ? a.getCreatedAt().toString() : "N/A",

                job != null ? job.getLocation() : "N/A",
                job != null ? job.getSalary() / 1000 : 0,
                job != null && job.getRecruiter() != null ? job.getRecruiter().getFullName() : "N/A",

                applicant != null && applicant.getLevel() != null ? applicant.getLevel().getValue() : "N/A",
                applicant != null && applicant.getGender() != null ? applicant.getGender().name() : "N/A"
        );
    }

    private String formatBlogContext(Blog blog) {
        return String.format(
                "- [%s](%s/blogs/%d) | Tác giả: %s | Tags: %s | Lượt xem: %d | Likes: %d | Comments: %d | Draft: %s | Enabled: %s | Ngày tạo: %s",
                blog.getTitle(),
                FE,
                blog.getBlogId(),
                blog.getAuthor() != null ? blog.getAuthor().getFullName() : "N/A",
                blog.getTags() != null && !blog.getTags().isEmpty()
                        ? String.join(", ", blog.getTags().subList(0, Math.min(3, blog.getTags().size())))
                        : "Không có",
                blog.getActivity() != null ? blog.getActivity().getTotalReads() : 0,
                blog.getActivity() != null ? blog.getActivity().getTotalLikes() : 0,
                blog.getActivity() != null ? blog.getActivity().getTotalComments() : 0,
                blog.isDraft() ? "Yes" : "No",
                blog.isEnabled() ? "Yes" : "No",
                blog.getCreatedAt() != null ? blog.getCreatedAt().toString() : "N/A"
        );
    }

    private String shorten(String text, int limit) {
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }
    // Format response


    // Lấy dữ liệu
    private String getJobsContextForRecruiter(Recruiter recruiter) {
        return this.getOrSet(
                "recruiterJobs" + recruiter.getUserId(),
                () -> {
                    Recruiter r = this.recruiterRepository.findById(recruiter.getUserId()).orElse(null);
                    if (r == null || r.getJobs().isEmpty()) return "Bạn chưa đăng job nào.";

                    return r.getJobs().stream().map(this::formatJobContext).collect(Collectors.joining("\n"));
                }
        );
    }

    @Override
    public String generateBlogDescription(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        String prompt = """
            Dựa trên nội dung bài viết sau, hãy tạo một mô tả ngắn gọn (khoảng 100-150 từ) để thu hút người đọc.
            Mô tả phải:
            - Tóm tắt ý chính của bài viết
            - Hấp dẫn và khơi gợi sự tò mò
            - Viết bằng Tiếng Việt
            - Không dùng ký tự đặc biệt hay markdown
            
            Nội dung bài viết:
            %s
            
            Chỉ trả về mô tả, không thêm gì khác.
            """.formatted(content);

        try {
            GenerateContentResponse response = this.client.models
                    .generateContent(MODEL, prompt, null);
            return response.text().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public List<String> generateBlogTags(String content) {
        if (content == null || content.trim().isEmpty()) {
            return List.of();
        }

        String prompt = """
            Dựa trên nội dung bài viết sau, hãy tạo 5-7 thẻ tag phù hợp.
            Các tag phải:
            - Ngắn gọn (1-3 từ)
            - Liên quan trực tiếp đến chủ đề
            - Viết bằng Tiếng Việt hoặc tiếng Anh (tùy ngữ cảnh)
            - Phân tách bằng dấu phẩy
            
            Nội dung bài viết:
            %s
            
            Chỉ trả về danh sách các tag cách nhau bằng dấu phẩy, không thêm gì khác.
            Ví dụ: Java, Spring Boot, Backend, API, Microservices
            """.formatted(content);

        try {
            GenerateContentResponse response = this.client.models
                    .generateContent(MODEL, prompt, null);
            String tagsText = response.text().trim();

            return List.of(tagsText.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .limit(7)
                    .toList();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @NotNull
    private static StringBuilder getStringBuilder(List<Message> messages) {
        StringBuilder historyBuilder = new StringBuilder();
        historyBuilder.append("\n--- LỊCH SỬ HỘI THOẠI ---\n");
    private String getApplicationsContextForRecruiter(Recruiter recruiter) {
        return this.getOrSet(
                "applicationsRecruiter" + recruiter.getUserId(),
                () -> {
                    Recruiter r = this.recruiterRepository.findById(recruiter.getUserId()).orElse(null);
                    if (r == null || r.getJobs().isEmpty()) return "Không có ứng tuyển nào.";

                    List<Long> jobIds = r.getJobs().stream().map(Job::getJobId).toList();
                    List<Application> apps = this.applicationRepository.findAll()
                            .stream()
                            .filter(app -> app.getJob() != null && jobIds.contains(app.getJob().getJobId()))
                            .toList();

                    if (apps.isEmpty()) return "Không có ứng viên nộp đơn.";

                    return apps.stream()
                            .map(app -> {
                                Job job = app.getJob();
                                Applicant applicant = app.getApplicant();

                                return String.format(
                                        "- Job: %s | Applicant: %s | Status: %s"
                                                + " | Email: %s | Resume: %s | Applied At: %s"
                                                + " | Job Location: %s | Job Salary: %.0fK | Recruiter: %s"
                                                + " | Applicant Level: %s | Applicant Gender: %s",

                                        job != null ? job.getTitle() : "N/A",
                                        applicant != null ? applicant.getFullName() : "N/A",
                                        app.getStatus() != null ? app.getStatus().getValue() : "N/A",

                                        app.getEmail() != null ? app.getEmail() : "N/A",
                                        app.getResumeUrl() != null ? app.getResumeUrl() : "N/A",
                                        app.getCreatedAt() != null ? app.getCreatedAt().toString() : "N/A",

                                        job != null ? job.getLocation() : "N/A",
                                        job != null ? job.getSalary() / 1000 : 0,
                                        job != null && job.getRecruiter() != null ? job.getRecruiter().getFullName() : "N/A",

                                        applicant != null && applicant.getLevel() != null ? applicant.getLevel().getValue() : "N/A",
                                        applicant != null && applicant.getGender() != null ? applicant.getGender().name() : "N/A"
                                );
                            })
                            .collect(Collectors.joining("\n"));
                }
        );

    }

    private String getRelevantApplicantsForRecruiter(Recruiter recruiter) {
        return this.getOrSet(
                "relevantApplicantsRecruiter" + recruiter.getUserId(),
                () -> {
                    Recruiter r = this.recruiterRepository.findById(recruiter.getUserId()).orElse(null);
                    if (r == null || r.getJobs().isEmpty()) return "Không có ứng viên phù hợp.";

                    Set<String> requiredSkills = r.getJobs().stream()
                            .flatMap(job -> job.getSkills().stream())
                            .map(Skill::getName)
                            .collect(Collectors.toSet());

                    if (requiredSkills.isEmpty()) return "Jobs của bạn chưa có skill requirements.";

                    List<Applicant> applicants = this.applicantRepository.findAll().stream()
                            .filter(Applicant::isAvailableStatus)
                            .filter(Applicant::isEnabled)
                            .toList();

                    if (applicants.isEmpty()) return "Không có ứng viên available.";

                    record ApplicantMatch(Applicant applicant, Set<String> matchedSkills, int matchCount) {}
                    List<ApplicantMatch> matches = new ArrayList<>();

                    for (Applicant applicant : applicants) {
                        Subscriber sub = this.subscriberRepository.findByEmail(applicant.getContact().getEmail());
                        if (sub == null || sub.getSkills().isEmpty()) continue;

                        Set<String> applicantSkills = sub.getSkills().stream()
                                .map(Skill::getName)
                                .collect(Collectors.toSet());

                        Set<String> matchedSkills = applicantSkills.stream()
                                .filter(requiredSkills::contains)
                                .collect(Collectors.toSet());

                        if (!matchedSkills.isEmpty()) {
                            matches.add(new ApplicantMatch(applicant, matchedSkills, matchedSkills.size()));
                        }
                    }

                    if (matches.isEmpty()) return "Không có ứng viên phù hợp với skill requirements.";

                    return matches.stream()
                            .sorted(Comparator.comparingInt(ApplicantMatch::matchCount).reversed())
                            .map(match -> String.format(
                                    "- %s | Level: %s | Matched Skills: %s (%d/%d)",
                                    match.applicant.getFullName(),
                                    match.applicant.getLevel() != null ? match.applicant.getLevel().getValue() : "N/A",
                                    String.join(", ", match.matchedSkills),
                                    match.matchCount,
                                    requiredSkills.size()
                            ))
                            .collect(Collectors.joining("\n"));
                }
        );

    }

    private String getApplicationsContextForApplicant(Applicant applicant) {
        return this.getOrSet(
                "applicationsApplicant" + applicant.getUserId(),
                () -> {
                    Applicant a = this.applicantRepository.findById(applicant.getUserId()).orElse(null);
                    if (a == null || a.getApplications().isEmpty()) return "Bạn chưa nộp đơn nào.";

                    return a.getApplications().stream()
                            .map(this :: formatApplicationContext)
                            .collect(Collectors.joining("\n"));
                }
        );
    }

    private String getRecommendedJobsForApplicant(Applicant applicant) {
        return this.getOrSet(
                "recommendedeJobsApplicant" + applicant.getUserId(),
                () -> {
                    Subscriber sub = this.subscriberRepository.findByEmail(applicant.getContact().getEmail());

                    if (sub == null || sub.getSkills().isEmpty()) {
                        return getTopJobsContext();
                    }

                    Set<String> userSkills = sub.getSkills().stream()
                            .map(Skill::getName)
                            .collect(Collectors.toSet());

                    record JobMatch(Job job, Set<String> matchedSkills, int matchCount, double matchPercentage) {}

                    List<Job> allJobs = this.jobRepository.findAll().stream()
                            .filter(Job::isActive)
                            .toList();

                    if (allJobs.isEmpty()) return "Không có công việc nào.";

                    List<JobMatch> matches = new ArrayList<>();

                    for (Job job : allJobs) {
                        if (job.getSkills().isEmpty()) continue;

                        Set<String> jobSkills = job.getSkills().stream()
                                .map(Skill::getName)
                                .collect(Collectors.toSet());

                        Set<String> matchedSkills = jobSkills.stream()
                                .filter(userSkills::contains)
                                .collect(Collectors.toSet());

                        if (!matchedSkills.isEmpty()) {
                            int matchCount = matchedSkills.size();
                            double matchPercentage = (double) matchCount / jobSkills.size() * 100;
                            matches.add(new JobMatch(job, matchedSkills, matchCount, matchPercentage));
                        }
                    }

                    if (matches.isEmpty()) {
                        return "Không có job phù hợp với skills của bạn.\n" + getTopJobsContext();
                    }

                    return matches.stream()
                            .sorted(Comparator
                                    .comparingDouble(JobMatch::matchPercentage)
                                    .reversed()
                                    .thenComparingInt(JobMatch::matchCount)
                                    .reversed())
                            .map(match -> String.format(
                                    "- [%s](%s/jobs/%d) | %.0fK VNĐ | %s | Match: %.0f%% (%d/%d) | Skills: %s",
                                    match.job.getTitle(),
                                    FE,
                                    match.job.getJobId(),
                                    match.job.getSalary() / 1000,
                                    match.job.getLevel().getValue(),
                                    match.matchPercentage,
                                    match.matchCount,
                                    match.job.getSkills().size(),
                                    String.join(", ", match.matchedSkills)
                            ))
                            .collect(Collectors.joining("\n"));
                }
        );
    }

    private String getSubscribersContextByApplicant(Applicant applicant) {
        return this.getOrSet(
                "subscribersApplicant" + applicant.getUserId(),
                () -> {
                    Applicant a = this.applicantRepository.findById(applicant.getUserId()).orElse(null);
                    if (a == null) return "Bạn chưa có đăng ký nào";

                    Subscriber sub = this.subscriberRepository.findByEmail(a.getContact().getEmail());
                    if (sub == null) return "Bạn chưa có đăng ký nào";

                    return String.format(
                            "- %s | Skills: %s",
                            sub.getName(),
                            sub.getSkills().stream()
                                    .map(Skill::getName)
                                    .collect(Collectors.joining(", "))
                    );
                }
        );
    }

    private String getOptimizedConversationHistory(Long conversationId, User currentUser) {
        return this.getOrSet(
                "conversationHistory" + conversationId + currentUser.getUserId(),
                () -> {
                    if (conversationId == null || currentUser == null) {
                        return "";
                    }

                    Conversation conversation = this.conversationService.handleGetConversationById(conversationId);
                    List<Message> messages = conversation.getMessages();

                    if (messages == null || messages.isEmpty()) {
                        return "";
                    }

                    StringBuilder history = new StringBuilder("\n--- LỊCH SỬ HỘI THOẠI (10 tin nhắn gần nhất) ---\n");
                    int startIndex = Math.max(0, messages.size() - 10);

                    for (int i = startIndex; i < messages.size(); i++) {
                        Message msg = messages.get(i);
                        String role = msg.getRole() == MessageRole.USER ? "User" : "AI";
                        history.append(String.format("%s: %s\n", role,
                                msg.getContent().length() > 200
                                        ? msg.getContent().substring(0, 200) + "..."
                                        : msg.getContent()
                        ));
                    }

                    return history.toString();
                }
        );
    }
    // Lấy dữ liệu


    // Gọi tới AI để lấy response
    private String callAiApi(String systemPrompt, String context, String history, String userMessage) {
        try {
            String fullPrompt = systemPrompt + context + history +
                    "\n\nHãy trả lời câu hỏi sau dựa trên dữ liệu context:\n\nUser: " + userMessage;

            GenerateContentResponse response = this.client.models
                    .generateContent(MODEL, fullPrompt, null);

            return response.text();
        } catch (Exception e) {
            e.printStackTrace();
            return "Xin lỗi, hiện tại tôi không thể xử lý yêu cầu. Vui lòng thử lại sau.";
        }
    }
    // Gọi tới AI để lấy response


    private String getOrSet(String key, Supplier<String> supplier ) {
        return this.cacheService.getOrSet(
                CACHE_NAME,
                key,
                String.class,
                supplier,
                TTL
        );
    }
}