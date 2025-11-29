package iuh.fit.goat.service;

import iuh.fit.goat.common.Role;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.ApplicantRepository;
import iuh.fit.goat.repository.ApplicationRepository;
import iuh.fit.goat.repository.JobRepository;
import iuh.fit.goat.repository.RecruiterRepository;
import iuh.fit.goat.util.SecurityUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
// Thêm import này để giải quyết LazyInitializationException
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final JobRepository jobRepository;
    private final ApplicantRepository applicantRepository;
    private final RecruiterRepository recruiterRepository;
    private final ApplicationRepository applicationRepository;
    private final UserService userService;

    public AiServiceImpl(ChatModel chatModel,
                         JobRepository jobRepository,
                         ApplicantRepository applicantRepository,
                         RecruiterRepository recruiterRepository,
                         ApplicationRepository applicationRepository,
                         UserService userService) {
        this.chatClient = ChatClient.create(chatModel);
        this.jobRepository = jobRepository;
        this.applicantRepository = applicantRepository;
        this.recruiterRepository = recruiterRepository;
        this.applicationRepository = applicationRepository;
        this.userService = userService;
    }

    /**
     * Xử lý chat AI với phân quyền động dựa trên vai trò người dùng.
     * - SUPER_ADMIN: Thấy tất cả.
     * - HR (Recruiter): Thấy công việc, ứng viên, và đơn ứng tuyển liên quan đến MÌNH.
     * - APPLICANT: Thấy công việc, và chỉ đơn ứng tuyển của CHÍNH MÌNH.
     * - GUEST (không đăng nhập): Chỉ thấy công việc.
     */
    @Transactional(readOnly = true)
    @Override
    public String chatWithAi(String userMessageContent) {
        // 1. Xác thực và lấy vai trò
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = SecurityUtil.getCurrentUserLogin().orElse(null);
        User currentUser = null;
        iuh.fit.goat.common.Role currentUserRole = null; // Sử dụng enum Role của bạn

        if (currentUserEmail != null) {
            currentUser = userService.handleGetUserByEmail(currentUserEmail);
            if (currentUser != null && currentUser.getRole() != null) {
                String roleName = currentUser.getRole().getName(); // Ví dụ: "SUPER_ADMIN", "HR", "APPLICANT"

                // So sánh với giá trị từ enum Role của bạn
                if (roleName.equals(Role.ADMIN.getValue())) {
                    currentUserRole = Role.ADMIN;
                } else if (roleName.equals(Role.RECRUITER.getValue())) {
                    currentUserRole = Role.RECRUITER;
                } else if (roleName.equals(Role.APPLICANT.getValue())) {
                    currentUserRole = Role.APPLICANT;
                }
            }
        }

        // 2. Xây dựng System Prompt dựa trên vai trò
        String systemPromptPreamble;
        String jobContext = "Bạn không có quyền xem thông tin này.";
        String applicantContext = "Bạn không có quyền xem thông tin này.";
        String recruiterContext = "Bạn không có quyền xem thông tin này.";
        String applicationContext = "Bạn không có quyền xem thông tin này.";

        if (currentUserRole == Role.ADMIN) {
            systemPromptPreamble = """
                Bạn là trợ lý AI thông minh của hệ thống "Goat Tìm Kiếm Việc Làm" và đang nói chuyện với một QUẢN TRỊ VIÊN (SUPER_ADMIN).
                Hãy trả lời ngắn gọn, chuyên nghiệp và chỉ sử dụng Tiếng Việt.
                Bạn có toàn quyền truy cập vào CSDL. Dưới đây là toàn bộ dữ liệu:
                """;
            jobContext = getAllJobsContext();
            applicantContext = getAllApplicantsContext();
            recruiterContext = getAllRecruitersContext();
            applicationContext = getAllApplicationsContext();

        } else if (currentUserRole == Role.RECRUITER) {
            systemPromptPreamble = String.format("""
                Bạn là trợ lý AI thông minh của hệ thống "Goat Tìm Kiếm Việc Làm" và đang nói chuyện với một NHÀ TUYỂN DỤNG (RECRUITER) có tên %s.
                Hãy trả lời ngắn gọn, thân thiện và chỉ sử dụng Tiếng Việt.
                CHỈ ĐƯỢC tư vấn dựa trên thông tin công việc, ứng viên và đơn ứng tuyển LIÊN QUAN ĐẾN NHÀ TUYỂN DỤNG NÀY.
                Nếu họ hỏi về dữ liệu của nhà tuyển dụng khác hoặc dữ liệu tổng, hãy lịch sự từ chối.
                Dưới đây là dữ liệu liên quan đến bạn:
                """, currentUser.getFullName());

            Recruiter currentRecruiter = (Recruiter) currentUser;
            jobContext = getJobsContextForRecruiter(currentRecruiter);
            applicationContext = getApplicationsContextForRecruiter(currentRecruiter);
            // HR có thể xem danh sách tất cả ứng viên để tìm kiếm tiềm năng
            applicantContext = getAllApplicantsContext();
            recruiterContext = "Bạn là Nhà Tuyển Dụng, bạn có thể xem thông tin của mình trong hồ sơ.";

        } else if (currentUserRole == Role.APPLICANT) {
            systemPromptPreamble = String.format("""
                Bạn là trợ lý AI thông minh của hệ thống "Goat Tìm Kiếm Việc Làm" và đang nói chuyện với một ỨNG VIÊN (APPLICANT) có tên %s.
                Hãy trả lời ngắn gọn, thân thiện và chỉ sử dụng Tiếng Việt.
                CHỈ ĐƯỢC tư vấn dựa trên danh sách công việc (Jobs) và các đơn ứng tuyển (Applications) CỦA CHÍNH ỨNG VIÊN NÀY.
                Nếu họ hỏi về ứng viên khác, nhà tuyển dụng khác (ngoài job họ xem), hoặc dữ liệu nội bộ, hãy lịch sự từ chối.
                Dưới đây là dữ liệu liên quan đến bạn:
                """, currentUser.getFullName());

            jobContext = getAllJobsContext(); // Ứng viên thấy tất cả các công việc
            Applicant currentApplicant = (Applicant) currentUser;
            applicationContext = getApplicationsContextForApplicant(currentApplicant);
            applicantContext = "Bạn là Ứng Viên, bạn có thể xem thông tin của mình trong hồ sơ.";
            recruiterContext = "Bạn có thể xem thông tin nhà tuyển dụng khi xem chi tiết công việc.";

        } else {
            // GUEST (không đăng nhập)
            systemPromptPreamble = """
                Bạn là trợ lý AI thông minh của hệ thống "Goat Tìm Kiếm Việc Làm" và đang nói chuyện với một KHÁCH (GUEST).
                Hãy trả lời ngắn gọn, thân thiện và chỉ sử dụng Tiếng Việt.
                CHỈ ĐƯỢC tư vấn dựa trên danh sách công việc (Jobs) đang có.
                Nếu họ hỏi về ứng viên, đơn ứng tuyển, hoặc dữ liệu nội bộ, hãy lịch sự từ chối và yêu cầu họ đăng nhập.
                Dưới đây là danh sách công việc công khai:
                """;
            jobContext = getAllJobsContext();
        }

        // 3. Tổng hợp System Prompt
        String systemText = systemPromptPreamble + """

            --- DỮ LIỆU NGỮ CẢNH ---

            [DANH SÁCH CÔNG VIỆC (JOBS)]:
            %s

            [DANH SÁCH ỨNG VIÊN (APPLICANTS)]:
            %s

            [DANH SÁCH NHÀ TUYỂN DỤNG (RECRUITERS)]:
            %s

            [DANH SÁCH ĐƠN ỨNG TUYỂN (APPLICATIONS)]:
            %s
            
            --- KẾT THÚC DỮ LIỆU ---
            Hãy trả lời câu hỏi của người dùng dựa TRÊN VÀ CHỈ TRÊN dữ liệu ngữ cảnh được cung cấp.
            """.formatted(jobContext, applicantContext, recruiterContext, applicationContext);

        // 4. Tạo Prompt và gọi AI
        SystemMessage systemMessage = new SystemMessage(systemText);
        UserMessage userMessage = new UserMessage(userMessageContent);

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        return chatClient.prompt(prompt).call().content();
    }

    // --- CÁC HÀM TẠO CONTEXT (HELPER METHODS) ---
    // Các hàm này phải là public/protected và @Transactional để tránh LazyInitializationException

    @Transactional(readOnly = true)
    @Override
    public String getAllJobsContext() {
        List<Job> jobs = jobRepository.findAll();
        if (jobs.isEmpty()) return "Không có công việc nào trong hệ thống.";
        return jobs.stream()
                .map(job -> String.format("- ID: %d, Tên: %s (Mức lương: %.0f, Cấp bậc: %s, Địa điểm: %s, Nhà tuyển dụng: %s)",
                        job.getJobId(),
                        job.getTitle(),
                        job.getSalary(),
                        job.getLevel(),
                        job.getLocation(),
                        job.getRecruiter() != null ? job.getRecruiter().getFullName() : "N/A"))
                .collect(Collectors.joining("\n"));
    }

    @Transactional(readOnly = true)
    @Override
    public String getAllApplicantsContext() {
        List<Applicant> applicants = applicantRepository.findAll();
        if (applicants.isEmpty()) return "Không có ứng viên nào.";
        return applicants.stream()
                .map(a -> String.format("- ID: %d, Tên: %s (Email: %s, Cấp bậc: %s, Trạng thái: %s)",
                        a.getUserId(),
                        a.getFullName(),
                        a.getContact().getEmail(),
                        a.getLevel(),
                        a.isAvailableStatus() ? "Sẵn sàng" : "Không sẵn sàng"))
                .collect(Collectors.joining("\n"));
    }

    @Transactional(readOnly = true)
    @Override
    public String getAllRecruitersContext() {
        // Lọc bỏ SUPER_ADMIN (userId = 1L) khỏi danh sách recruiter
        List<Recruiter> recruiters = recruiterRepository.findAll().stream()
                .filter(r -> r.getUserId() != 1L)
                .toList();
        if (recruiters.isEmpty()) return "Không có nhà tuyển dụng nào (ngoại trừ Admin).";
        return recruiters.stream()
                .map(r -> String.format("- ID: %d, Tên: %s (Email: %s, Website: %s)",
                        r.getUserId(),
                        r.getFullName(),
                        r.getContact().getEmail(),
                        r.getWebsite()))
                .collect(Collectors.joining("\n"));
    }

    @Transactional(readOnly = true)
    @Override
    public String getAllApplicationsContext() {
        List<Application> applications = applicationRepository.findAll();
        if (applications.isEmpty()) return "Không có đơn ứng tuyển nào.";
        return applications.stream()
                .map(app -> String.format("- ID: %d, Công việc: %s (ID: %d), Ứng viên: %s (ID: %d), Trạng thái: %s",
                        app.getApplicationId(),
                        app.getJob() != null ? app.getJob().getTitle() : "N/A",
                        app.getJob() != null ? app.getJob().getJobId() : 0,
                        app.getApplicant() != null ? app.getApplicant().getFullName() : "N/A",
                        app.getApplicant() != null ? app.getApplicant().getUserId() : 0,
                        app.getStatus()))
                .collect(Collectors.joining("\n"));
    }

    @Transactional(readOnly = true)
    @Override
    public String getJobsContextForRecruiter(Recruiter recruiter) {
        // Cần fetch lại recruiter trong transaction để tránh LazyInitializationException
        Recruiter managedRecruiter = recruiterRepository.findById(recruiter.getUserId()).orElse(null);
        if (managedRecruiter == null || managedRecruiter.getJobs() == null || managedRecruiter.getJobs().isEmpty()) {
            return "Nhà tuyển dụng này chưa đăng công việc nào.";
        }
        return managedRecruiter.getJobs().stream()
                .map(job -> String.format("- ID: %d, Tên: %s (Mức lương: %.0f, Cấp bậc: %s, Trạng thái: %s)",
                        job.getJobId(),
                        job.getTitle(),
                        job.getSalary(),
                        job.getLevel(),
                        job.isActive() ? "Đang mở" : "Đã đóng"))
                .collect(Collectors.joining("\n"));
    }

    @Transactional(readOnly = true)
    @Override
    public String getApplicationsContextForRecruiter(Recruiter recruiter) {
        Recruiter managedRecruiter = recruiterRepository.findById(recruiter.getUserId()).orElse(null);
        if (managedRecruiter == null || managedRecruiter.getJobs() == null || managedRecruiter.getJobs().isEmpty()) {
            return "Không có đơn ứng tuyển nào (vì nhà tuyển dụng chưa đăng việc).";
        }

        // Lấy ID các công việc của HR này
        List<Long> jobIds = managedRecruiter.getJobs().stream().map(Job::getJobId).toList();
        if (jobIds.isEmpty()) {
            return "Không có đơn ứng tuyển nào (vì nhà tuyển dụng chưa đăng việc).";
        }

        // Tìm tất cả đơn ứng tuyển cho các công việc đó
        // Đây là cách tối ưu hơn thay vì tải tất cả applications
        List<Application> applications = applicationRepository.findAll().stream()
                .filter(app -> app.getJob() != null && jobIds.contains(app.getJob().getJobId()))
                .toList();

        if (applications.isEmpty()) return "Chưa có ứng viên nào nộp đơn cho các công việc của bạn.";

        return applications.stream()
                .map(app -> String.format("- ID: %d, Công việc: %s, Ứng viên: %s (Email: %s), Trạng thái: %s, CV: %s",
                        app.getApplicationId(),
                        app.getJob().getTitle(),
                        app.getApplicant() != null ? app.getApplicant().getFullName() : "N/A",
                        app.getApplicant() != null ? app.getApplicant().getContact().getEmail() : "N/A",
                        app.getStatus(),
                        app.getResumeUrl()))
                .collect(Collectors.joining("\n"));
    }

    @Transactional(readOnly = true)
    @Override
    public String getApplicationsContextForApplicant(Applicant applicant) {
        Applicant managedApplicant = applicantRepository.findById(applicant.getUserId()).orElse(null);
        if (managedApplicant == null || managedApplicant.getApplications() == null || managedApplicant.getApplications().isEmpty()) {
            return "Bạn chưa nộp đơn ứng tuyển nào.";
        }
        return managedApplicant.getApplications().stream()
                .map(app -> String.format("- ID: %d, Công việc: %s (Nhà tuyển dụng: %s), Trạng thái: %s, Ngày nộp: %s",
                        app.getApplicationId(),
                        app.getJob() != null ? app.getJob().getTitle() : "N/A",
                        app.getJob() != null && app.getJob().getRecruiter() != null ? app.getJob().getRecruiter().getFullName() : "N/A",
                        app.getStatus(),
                        app.getCreatedAt().toString()))
                .collect(Collectors.joining("\n"));
    }
}