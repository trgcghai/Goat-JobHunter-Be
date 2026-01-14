package iuh.fit.goat.service.impl;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import iuh.fit.goat.common.Role;
import iuh.fit.goat.dto.request.ai.ChatRequest;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.service.*;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {
    private final Client client;

    private final AccountService accountService;
    private final CacheService cacheService;

    private final JobRepository jobRepository;
    private final ApplicantRepository applicantRepository;
    private final ApplicationRepository applicationRepository;
    private final SubscriberRepository subscriberRepository;
    private final BlogRepository blogRepository;
    private final CareerRepository careerRepository;
    private final SkillRepository skillRepository;
    private final CompanyRepository companyRepository;

    @Value("${google.api.model}")
    private String MODEL;
    @Value("${goat.fe.url}")
    private String FE;

    private final String CACHE_NAME = "aiChat";
    private final Long TTL = 900L;

    @Override
    @Transactional
    public String chatWithAi(ChatRequest request) {
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();
        Account currentAccount = null;
        Role currentUserRole = null;

        if (currentUserEmail != null) {
            currentAccount = this.accountService.handleGetAccountByEmail(currentUserEmail);
            if (currentAccount != null && currentAccount.getRole() != null) {
                currentUserRole = getRoleFromUser(currentAccount);
            }
        }

        String systemPrompt = buildSystemPrompt(currentAccount, currentUserRole);
        String contextData = buildSmartContext(currentAccount, currentUserRole, request.getMessage());

        String aiResponse = callAiApi(systemPrompt, contextData, "", request.getMessage());

        return aiResponse;
    }

    // Lấy vai trò của current user
    private Role getRoleFromUser(Account account) {
        String roleName = account.getRole().getName();
        if (roleName.equals(iuh.fit.goat.common.Role.ADMIN.getValue())) return Role.ADMIN;
        if (roleName.equals(Role.COMPANY.getValue())) return Role.COMPANY;
        if (roleName.equals(Role.APPLICANT.getValue())) return Role.APPLICANT;
        return null;
    }
    // Lấy vai trò của current user

//    // Build prompt cho user phù hợp với từng vai trò
    private String buildSystemPrompt(Account currentAccount, Role currentUserRole) {
        String basePrompt = """
                Bạn là trợ lý AI thông minh của hệ thống "Goat Tìm Kiếm Việc Làm".
                Current date: %s.\n
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
            return basePrompt + "\nQuyền: ADMIN - toàn bộ dữ liệu.";
        } else if (currentUserRole == Role.COMPANY) {
            return basePrompt + String.format("\nQuyền: Company %s - chỉ dữ liệu liên quan đến job (có thể là job của company khác).",
                    ((Company)currentAccount).getName());
        } else if (currentUserRole == Role.APPLICANT) {
            return basePrompt + String.format("\nQuyền: Applicant %s - tư vấn job và application.",
                    ((Applicant)currentAccount).getFullName());
        } else {
            return basePrompt + "\nQuyền: GUEST - chỉ job công khai.";
        }
    }

    private String buildSmartContext(Account currentAccount, Role currentUserRole, String message) {
        StringBuilder context = new StringBuilder("\n--- DỮ LIỆU NGỮ CẢNH ---\n");

        if (currentUserRole == Role.ADMIN) {
            context.append(buildAdminContext(message));
        } else if (currentUserRole == Role.COMPANY) {
            context.append(buildCompanyContext((Company) currentAccount, message));
        } else if (currentUserRole == Role.APPLICANT) {
            context.append(buildApplicantContext((Applicant) currentAccount, message));
        } else {
            context.append(buildGuestContext(message));
        }

        return context.toString();
    }

    private String buildAdminContext(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("[STATISTICS]\n").append(getSystemStatsContext()).append("\n\n");

        if (containsKeywords(message, "job", "việc", "tuyển dụng", "công việc")) {
            sb.append("[RECRUITERS]\n").append(getTopCompaniesContext()).append("\n\n");
            sb.append("[TOP JOBS]\n").append(getTopJobsContext()).append("\n\n");
        }
        if (containsKeywords(message, "ứng viên", "applicant", "candidate")) {
            sb.append("[APPLICATIONS]\n").append(getRecentApplicationsContext()).append("\n\n");
            sb.append("[TOP APPLICANTS]\n").append(getTopApplicantsContext()).append("\n\n");
        }
        if (containsKeywords(message, "recruiter", "nhà tuyển dụng", "công ty")) {
            sb.append("[COMPANIES]\n").append(getTopCompaniesContext()).append("\n\n");
            sb.append("[TOP JOBS]\n").append(getTopJobsContext()).append("\n\n");
        }
        if (containsKeywords(message, "application", "đơn", "ứng tuyển")) {
            sb.append("[APPLICATIONS]\n").append(getRecentApplicationsContext()).append("\n\n");
            sb.append("[TOP APPLICANTS]\n").append(getTopApplicantsContext()).append("\n\n");
        }
        if (containsKeywords(message, "skill", "kỹ năng")) {
            sb.append("[SKILLS]\n").append(getTopSkillsContext()).append("\n\n");
        }
        if (containsKeywords(message, "blog", "bài viết")) {
            sb.append("[BLOGS]\n").append(getRecentBlogsContext()).append("\n\n");
        }
        if (containsKeywords(message, "career", "ngành", "lĩnh vực")) {
            sb.append("[CAREERS]\n").append(getAllCareersContext()).append("\n\n");
        }

        return sb.toString();
    }

    private String buildCompanyContext(Company company, String message) {
        StringBuilder sb = new StringBuilder();

        sb.append("[MY JOBS]\n").append(getJobsContextForCompany(company)).append("\n\n");
        sb.append("[TOP JOBS]\n").append(getTopJobsContext()).append("\n\n");

        if (containsKeywords(message, "application", "đơn", "ứng tuyển", "ứng viên")) {
            sb.append("[APPLICATIONS]\n").append(getApplicationsContextForCompany(company)).append("\n\n");
            sb.append("[RELEVANT APPLICANTS]\n").append(getRelevantApplicantsForCompany(company)).append("\n\n");
        }
        if (containsKeywords(message, "skill", "kỹ năng")) {
            sb.append("[SKILLS]\n").append(getTopSkillsContext()).append("\n\n");
        }
        if (containsKeywords(message, "blog", "bài viết")) {
            sb.append("[BLOGS]\n").append(getRecentBlogsContext()).append("\n\n");
        }
        if (containsKeywords(message, "career", "ngành")) {
            sb.append("[CAREERS]\n").append(getAllCareersContext()).append("\n\n");
        }

        return sb.toString();
    }

    private String buildApplicantContext(Applicant applicant, String message) {
        StringBuilder sb = new StringBuilder();

        sb.append("[MY APPLICATIONS]\n").append(getApplicationsContextForApplicant(applicant)).append("\n\n");
        sb.append("[SUBSCRIPTIONS]\n").append(getSubscribersContextByApplicant(applicant)).append("\n\n");

        if (containsKeywords(message, "job", "việc", "tuyển dụng", "gợi ý")) {
            sb.append("[RECOMMENDED JOBS]\n").append(getRecommendedJobsForApplicant(applicant)).append("\n\n");
        }
        if (containsKeywords(message, "skill", "kỹ năng")) {
            sb.append("[SKILLS]\n").append(getTopSkillsContext()).append("\n\n");
        }
        if (containsKeywords(message, "blog", "bài viết")) {
            sb.append("[BLOGS]\n").append(getRecentBlogsContext()).append("\n\n");
        }
        if (containsKeywords(message, "career", "ngành")) {
            sb.append("[CAREERS]\n").append(getAllCareersContext()).append("\n\n");
        }

        return sb.toString();
    }

    private String buildGuestContext(String message) {
        StringBuilder sb = new StringBuilder();

        sb.append("[AVAILABLE JOBS]\n").append(getTopJobsContext()).append("\n\n");

        if (containsKeywords(message, "skill", "kỹ năng")) {
            sb.append("[TRENDING SKILLS]\n").append(getTopSkillsContext()).append("\n\n");
        }
        if (containsKeywords(message, "blog", "bài viết")) {
            sb.append("[BLOGS]\n").append(getRecentBlogsContext()).append("\n\n");
        }
        if (containsKeywords(message, "career", "ngành")) {
            sb.append("[CAREERS]\n").append(getAllCareersContext()).append("\n\n");
        }
        if (containsKeywords(message, "thống kê", "tổng quan", "overview")) {
            sb.append("[OVERVIEW]\n").append(getJobMarketOverview()).append("\n\n");
        }

        return sb.toString();
    }
//    // Build prompt cho user phù hợp với từng vai trò


//    // Lấy dữ liệu lưu vào cache
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
    public String getTopCompaniesContext() {
        return this.getOrSet(
                "allCompanies",
                () -> {
                    List<Company> companies = this.companyRepository.findAll()
                            .stream()
                            .filter(c -> c.getAccountId() != 1L)
                            .toList();
                    if (companies.isEmpty()) return "Không có công ty.";

                    return companies.stream().map(this::formatCompanyContext).collect(Collectors.joining("\n"));
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
                                    - Tổng Companies: %d
                                    - Tổng Applications: %d
                                    - Jobs Active: %d
                                    - Tổng Blogs: %d
                                    - Tổng Careers: %d
                                    - Tổng Skills: %d
                                    """,
                        this.jobRepository.count(),
                        this.applicantRepository.count(),
                        this.companyRepository.count(),
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
                                    .collect(Collectors.groupingBy(job -> job.getAddress().getProvince(), Collectors.counting()))
                                    .entrySet().stream()
                                    .max(Map.Entry.comparingByValue())
                                    .map(Map.Entry::getKey)
                                    .orElse("N/A")
                    );
                }
        );
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
                job.getAddress().getFullAddress(),
                job.getSkills().stream()
                        .map(Skill::getName)
                        .collect(Collectors.joining(", ")),
                job.getCompany() != null ? job.getCompany().getName() : "N/A",
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
                a.getPhone() != null ? a.getPhone() : "N/A",
                a.getEmail() != null ? a.getEmail() : "N/A",
                a.getResumes() != null ? a.getResumes().stream().map(Resume::getFileUrl).collect(Collectors.joining(", ")) : "N/A",
                a.getApplications() != null ? a.getApplications().size() : 0,
                a.isEnabled() ? "Yes" : "No"
        );
    }

    private String formatCompanyContext(Company c) {
        return String.format(
                "- %s | %s | Description: %s | Phone: %s | Email: %s | Address: %s | Jobs: %d | Followers: %d | Enabled: %s",
                c.getName(),
                c.getWebsite() != null ? c.getWebsite() : "N/A",
                c.getDescription() != null ? shorten(c.getDescription(), 120) : "N/A",
                c.getPhone() != null ? c.getPhone() : "N/A",
                c.getEmail() != null ? c.getEmail() : "N/A",
                c.getAddresses() != null ? c.getAddresses().stream().map(Address::getFullAddress).collect(Collectors.joining(" ")) : "N/A",
                c.getJobs() != null ? c.getJobs().size() : 0,
                c.getFollowers() != null ? c.getFollowers().size() : 0,
                c.isEnabled() ? "Yes" : "No"
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
                a.getResume().getFileUrl() != null ? a.getResume().getFileUrl() : "N/A",
                a.getCreatedAt() != null ? a.getCreatedAt().toString() : "N/A",

                job != null ? job.getAddress().getFullAddress() : "N/A",
                job != null ? job.getSalary() / 1000 : 0,
                job != null && job.getCompany() != null ? job.getCompany().getName() : "N/A",

                applicant != null && applicant.getLevel() != null ? applicant.getLevel().getValue() : "N/A",
                applicant != null && applicant.getGender() != null ? applicant.getGender().name() : "N/A"
        );
    }

    private String formatBlogContext(Blog blog) {
        return String.format(
                "- [%s](%s/blogs/%d) | Tác giả: %s | Tags: %s | Lượt xem: %d | Likes: %d | Comments: %d | Enabled: %s | Ngày tạo: %s",
                blog.getContent(),
                FE,
                blog.getBlogId(),
                blog.getAuthor() != null ? blog.getAuthor().getFullName() : "N/A",
                blog.getTags() != null && !blog.getTags().isEmpty()
                        ? String.join(", ", blog.getTags().subList(0, Math.min(3, blog.getTags().size())))
                        : "Không có",
                blog.getActivity() != null ? blog.getActivity().getTotalReads() : 0,
                blog.getActivity() != null ? blog.getActivity().getTotalLikes() : 0,
                blog.getActivity() != null ? blog.getActivity().getTotalComments() : 0,
                blog.isEnabled() ? "Yes" : "No",
                blog.getCreatedAt() != null ? blog.getCreatedAt().toString() : "N/A"
        );
    }

    private String shorten(String text, int limit) {
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }
    // Format response


    // Lấy dữ liệu
    private String getJobsContextForCompany(Company company) {
        return this.getOrSet(
                "companyJobs" + company.getAccountId(),
                () -> {
                    Company c = this.companyRepository.findById(company.getAccountId()).orElse(null);
                    if (c == null || c.getJobs().isEmpty()) return "Bạn chưa đăng job nào.";

                    return c.getJobs().stream().map(this::formatJobContext).collect(Collectors.joining("\n"));
                }
        );
    }

    private String getApplicationsContextForCompany(Company company) {
        return this.getOrSet(
                "applicationsCompany" + company.getAccountId(),
                () -> {
                    Company c = this.companyRepository.findById(company.getAccountId()).orElse(null);
                    if (c == null || c.getJobs().isEmpty()) return "Không có ứng tuyển nào.";

                    List<Long> jobIds = c.getJobs().stream().map(Job::getJobId).toList();
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
                                        app.getResume().getFileUrl() != null ? app.getResume().getFileUrl() : "N/A",
                                        app.getCreatedAt() != null ? app.getCreatedAt().toString() : "N/A",

                                        job != null ? job.getAddress().getFullAddress() : "N/A",
                                        job != null ? job.getSalary() / 1000 : 0,
                                        job != null && job.getCompany() != null ? job.getCompany().getName() : "N/A",

                                        applicant != null && applicant.getLevel() != null ? applicant.getLevel().getValue() : "N/A",
                                        applicant != null && applicant.getGender() != null ? applicant.getGender().name() : "N/A"
                                );
                            })
                            .collect(Collectors.joining("\n"));
                }
        );

    }

    private String getRelevantApplicantsForCompany(Company company) {
        return this.getOrSet(
                "relevantApplicantsCompany" + company.getAccountId(),
                () -> {
                    Company c = this.companyRepository.findById(company.getAccountId()).orElse(null);
                    if (c == null || c.getJobs().isEmpty()) return "Không có ứng viên phù hợp.";

                    Set<String> requiredSkills = c.getJobs().stream()
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
                        Subscriber sub = this.subscriberRepository.findByEmail(applicant.getEmail());
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
                "applicationsApplicant" + applicant.getAccountId(),
                () -> {
                    Applicant a = this.applicantRepository.findById(applicant.getAccountId()).orElse(null);
                    if (a == null || a.getApplications().isEmpty()) return "Bạn chưa nộp đơn nào.";

                    return a.getApplications().stream()
                            .map(this :: formatApplicationContext)
                            .collect(Collectors.joining("\n"));
                }
        );
    }

    private String getRecommendedJobsForApplicant(Applicant applicant) {
        return this.getOrSet(
                "recommendedeJobsApplicant" + applicant.getAccountId(),
                () -> {
                    Subscriber sub = this.subscriberRepository.findByEmail(applicant.getEmail());

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
                "subscribersApplicant" + applicant.getAccountId(),
                () -> {
                    Applicant a = this.applicantRepository.findById(applicant.getAccountId()).orElse(null);
                    if (a == null) return "Bạn chưa có đăng ký nào";

                    Subscriber sub = this.subscriberRepository.findByEmail(a.getEmail());
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
    // Lấy dữ liệu



    // Gọi tới AI để lấy response
    private String callAiApi(String systemPrompt, String context, String history, String userMessage) {
        try {
            String fullPrompt = systemPrompt + context + history +
                    "\n\nHãy trả lời câu hỏi sau dựa trên dữ liệu context(nếu có):\n\nUser: " + userMessage;

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

    private boolean containsKeywords(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}