package iuh.fit.goat.service.impl;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import iuh.fit.goat.common.MessageRole;
import iuh.fit.goat.common.Role;
import iuh.fit.goat.dto.request.ai.ChatRequest;
import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.service.AiService;
import iuh.fit.goat.service.ConversationService;
import iuh.fit.goat.service.MessageService;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {
    private final Client client;

    private final UserService userService;
    private final ConversationService conversationService;
    private final MessageService messageService;

    private final JobRepository jobRepository;
    private final ApplicantRepository applicantRepository;
    private final RecruiterRepository recruiterRepository;
    private final ApplicationRepository applicationRepository;

    @Value("${google.api.model}")
    private String MODEL;

    @Override
    @Transactional
    public String chatWithAi(ChatRequest request) {
        String currentUserEmail = SecurityUtil.getCurrentUserLogin().orElse(null);

        User currentUser = null;
        Role currentUserRole = null;

        if (currentUserEmail != null) {
            currentUser = this.userService.handleGetUserByEmail(currentUserEmail);

            if (currentUser != null && currentUser.getRole() != null) {
                String roleName = currentUser.getRole().getName();

                if (roleName.equals(Role.ADMIN.getValue())) currentUserRole = Role.ADMIN;
                else if (roleName.equals(Role.RECRUITER.getValue())) currentUserRole = Role.RECRUITER;
                else if (roleName.equals(Role.APPLICANT.getValue())) currentUserRole = Role.APPLICANT;
            }
        }

        String systemPromptPreamble = """
            Bạn là trợ lý AI thông minh của hệ thống "Goat Tìm Kiếm Việc Làm".
            Bạn có quyền truy cập dữ liệu nội bộ GOAT (Jobs, Applicants, Recruiters, Applications).
            
            Current date and time: %s.
            
            Instructions:
            1. Bạn có thể trả lời bất kỳ câu hỏi nào, về GOAT hoặc kiến thức tổng quát: thể thao, khoa học, lịch sử, vũ trụ, chính trị, công nghệ, giải trí, v.v.
            2. Khi câu hỏi liên quan đến dữ liệu GOAT (công việc, ứng viên, nhà tuyển dụng, đơn ứng tuyển), ưu tiên sử dụng dữ liệu nội bộ để trả lời.
            3. Khi câu hỏi không liên quan đến GOAT, trả lời dựa trên kiến thức tổng quát, chính xác, đầy đủ.
            4. Luôn trả lời Tiếng Việt, thân thiện, tự nhiên, dễ hiểu và ngắn gọn.
            5. Nếu câu hỏi yêu cầu ví dụ, minh họa hoặc phân tích, hãy đưa ví dụ ngắn gọn, dễ hiểu.
            6. Không nói rằng bạn không biết trừ khi thông tin thực sự không có (ví dụ dữ liệu thời gian thực mà AI không thể cập nhật).
            7. Khi trả lời câu hỏi GOAT, chỉ sử dụng dữ liệu mà người dùng có quyền xem dựa trên vai trò của họ (ADMIN, RECRUITER, APPLICANT, GUEST).
            8. Tham khảo lịch sử hội thoại trước đó để trả lời mạch lạc, liên tục và có ngữ cảnh.
            """
            .formatted(new Date().toString());

        String jobContext = "Bạn không có quyền xem thông tin này.";
        String applicantContext = "Bạn không có quyền xem thông tin này.";
        String recruiterContext = "Bạn không có quyền xem thông tin này.";
        String applicationContext = "Bạn không có quyền xem thông tin này.";

        if (currentUserRole == Role.ADMIN) {
            systemPromptPreamble = systemPromptPreamble + """
                Bạn đang làm việc với SUPER_ADMIN.
                """;

            jobContext = getAllJobsContext();
            applicantContext = getAllApplicantsContext();
            recruiterContext = getAllRecruitersContext();
            applicationContext = getAllApplicationsContext();

        } else if (currentUserRole == Role.RECRUITER) {
            systemPromptPreamble = systemPromptPreamble + String.format("""
                Bạn đang hỗ trợ một Nhà Tuyển Dụng tên %s.
                Chỉ được sử dụng dữ liệu liên quan đến họ.
                """, currentUser.getFullName()
            );

            Recruiter r = (Recruiter) currentUser;
            jobContext = getJobsContextForRecruiter(r);
            applicantContext = getAllApplicantsContext();
            applicationContext = getApplicationsContextForRecruiter(r);
            recruiterContext = "Thông tin của bạn nằm trong hồ sơ.";

        } else if (currentUserRole == Role.APPLICANT) {
            systemPromptPreamble = systemPromptPreamble + String.format("""
                Bạn đang nói chuyện với ứng viên %s.
                Chỉ được tư vấn dựa trên công việc và đơn ứng tuyển của chính họ.
                """, currentUser.getFullName()
            );

            jobContext = getAllJobsContext();
            Applicant a = (Applicant) currentUser;
            applicationContext = getApplicationsContextForApplicant(a);
            applicantContext = "Bạn là ứng viên.";
            recruiterContext = "Bạn chỉ xem được recruiter trong từng job.";

        } else {
            systemPromptPreamble = systemPromptPreamble + """
                Bạn đang hỗ trợ khách GUEST.
                Chỉ được dùng dữ liệu công việc.
                """;

            jobContext = getAllJobsContext();
        }

        String conversationHistory = "";
        if(request.getConversationId() != null && currentUser != null) {
            conversationHistory = getConversationHistory(request.getConversationId());
        }

        String finalPrompt = systemPromptPreamble + """

            --- DỮ LIỆU NGỮ CẢNH ---
            [JOBS]
            %s

            [APPLICANTS]
            %s

            [RECRUITERS]
            %s

            [APPLICATIONS]
            %s
            %s

            Hãy trả lời đúng dựa trên dữ liệu NGỮ CẢNH ở trên.
            """
                .formatted(jobContext, applicantContext, recruiterContext, applicationContext, conversationHistory);

        if(request.getConversationId() != null && currentUser != null) {
            this.messageService.handleCreateMessage(new MessageCreateRequest(
                    request.getConversationId(), MessageRole.USER, request.getMessage()
            ));
        }

        String aiResponse;
        try {
            GenerateContentResponse response = this.client.models
                    .generateContent(MODEL, finalPrompt + "\n\nUser: " + request.getMessage(), null);
            aiResponse = response.text();
        } catch (Exception e) {
            e.printStackTrace();
            aiResponse = "Xin lỗi, hiện tại tôi không thể xử lý yêu cầu. Vui lòng thử lại sau.";
        }

        if(request.getConversationId() != null && currentUser != null) {
            this.messageService.handleCreateMessage(new MessageCreateRequest(
                    request.getConversationId(), MessageRole.AI, aiResponse
            ));
            this.conversationService.handleUpdateTitleIfFirstAiMessage(
                    request.getConversationId(), aiResponse
            );
        }

        return aiResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public String getAllJobsContext() {
        List<Job> jobs = this.jobRepository.findAll();
        if (jobs.isEmpty()) return "Không có công việc nào.";

        return jobs.stream()
                .map(job -> String.format(
                        "- ID: %d, %s | Lương %.0f | %s | %s | Recruiter: %s",
                        job.getJobId(),
                        job.getTitle(),
                        job.getSalary(),
                        job.getLevel().getValue(),
                        job.getLocation(),
                        job.getRecruiter() != null ? job.getRecruiter().getFullName() : "N/A"
                ))
                .collect(Collectors.joining("\n"));
    }

    @Override
    @Transactional(readOnly = true)
    public String getAllApplicantsContext() {
        List<Applicant> applicants = this.applicantRepository.findAll();
        if (applicants.isEmpty()) return "Không có ứng viên.";

        return applicants.stream()
                .map(a -> String.format(
                        "- ID: %d, %s | Email %s | Level %s | %s",
                        a.getUserId(),
                        a.getFullName(),
                        a.getContact().getEmail(),
                        a.getLevel() != null ? a.getLevel().getValue() : "N/A",
                        a.isAvailableStatus() ? "Available" : "Busy"
                ))
                .collect(Collectors.joining("\n"));
    }

    @Override
    @Transactional(readOnly = true)
    public String getAllRecruitersContext() {
        List<Recruiter> recruiters = this.recruiterRepository.findAll().stream()
                .filter(r -> r.getUserId() != 1L)
                .toList();

        if (recruiters.isEmpty()) return "Không có recruiter.";

        return recruiters.stream()
                .map(r -> String.format(
                        "- ID: %d, %s | Email %s | Website %s",
                        r.getUserId(),
                        r.getFullName(),
                        r.getContact().getEmail(),
                        r.getWebsite()
                ))
                .collect(Collectors.joining("\n"));
    }

    @Override
    @Transactional(readOnly = true)
    public String getAllApplicationsContext() {
        List<Application> apps = this.applicationRepository.findAll();
        if (apps.isEmpty()) return "Không có đơn ứng tuyển.";

        return apps.stream()
                .map(app -> String.format(
                        "- ID: %d | Job %s | Applicant %s | %s",
                        app.getApplicationId(),
                        app.getJob() != null ? app.getJob().getTitle() : "N/A",
                        app.getApplicant() != null ? app.getApplicant().getFullName() : "N/A",
                        app.getStatus()
                ))
                .collect(Collectors.joining("\n"));
    }

    @Override
    @Transactional(readOnly = true)
    public String getJobsContextForRecruiter(Recruiter recruiter) {
        Recruiter r = this.recruiterRepository.findById(recruiter.getUserId()).orElse(null);
        if (r == null || r.getJobs().isEmpty()) return "Bạn chưa đăng job nào.";

        return r.getJobs().stream()
                .map(job -> String.format(
                        "- ID: %d, %s | %.0f | %s | %s",
                        job.getJobId(),
                        job.getTitle(),
                        job.getSalary(),
                        job.getLevel().getValue(),
                        job.isActive() ? "Đang mở" : "Đã đóng"
                ))
                .collect(Collectors.joining("\n"));
    }

    @Override
    @Transactional(readOnly = true)
    public String getApplicationsContextForRecruiter(Recruiter recruiter) {
        Recruiter r = this.recruiterRepository.findById(recruiter.getUserId()).orElse(null);
        if (r == null || r.getJobs().isEmpty()) return "Không có ứng tuyển nào.";

        List<Long> jobIds = r.getJobs().stream().map(Job::getJobId).toList();

        List<Application> apps = this.applicationRepository.findAll()
                .stream()
                .filter(app -> app.getJob() != null && jobIds.contains(app.getJob().getJobId()))
                .toList();

        if (apps.isEmpty()) return "Không có ứng viên nộp đơn.";

        return apps.stream()
                .map(app -> String.format(
                        "- ID: %d | Job %s | Applicant %s | %s",
                        app.getApplicationId(),
                        app.getJob().getTitle(),
                        app.getApplicant().getFullName(),
                        app.getStatus()
                ))
                .collect(Collectors.joining("\n"));
    }

    @Override
    @Transactional(readOnly = true)
    public String getApplicationsContextForApplicant(Applicant applicant) {
        Applicant a = this.applicantRepository.findById(applicant.getUserId()).orElse(null);
        if (a == null || a.getApplications().isEmpty()) return "Bạn chưa nộp đơn nào.";

        return a.getApplications().stream()
                .map(app -> String.format(
                        "- ID: %d | Job %s | Recruiter %s | %s",
                        app.getApplicationId(),
                        app.getJob().getTitle(),
                        app.getJob().getRecruiter().getFullName(),
                        app.getStatus()
                ))
                .collect(Collectors.joining("\n"));
    }

    @Override
    @Transactional(readOnly = true)
    public String getConversationHistory(Long conversationId) {
        String conversationHistory = "";
        Conversation conversation = this.conversationService.handleGetConversationById(conversationId);
        List<Message> messages = conversation.getMessages();

        if (messages != null && !messages.isEmpty()) {
            StringBuilder historyBuilder = getStringBuilder(messages);
            conversationHistory = historyBuilder.toString();
        }
        return conversationHistory;
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

        int startIndex = Math.max(0, messages.size() - 50);
        for (int i = startIndex; i < messages.size(); i++) {
            Message msg = messages.get(i);
            String role = msg.getRole() == MessageRole.USER ? "User" : "AI";
            historyBuilder.append(String.format("%s: %s\n", role, msg.getContent()));
        }

        return historyBuilder;
    }
}