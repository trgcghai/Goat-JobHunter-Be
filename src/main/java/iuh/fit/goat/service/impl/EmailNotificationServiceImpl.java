package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.ActionType;
import iuh.fit.goat.entity.Application;
import iuh.fit.goat.enumeration.Status;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.repository.ApplicantRepository;
import iuh.fit.goat.repository.JobRepository;
import iuh.fit.goat.service.AsyncEmailService;
import iuh.fit.goat.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailNotificationServiceImpl implements EmailNotificationService {
    private final SpringTemplateEngine templateEngine;
    private final AsyncEmailService asyncEmailService;
//    private final ApplicantRepository applicantRepository;
//    private final JobRepository jobRepository;
//
//
//    @Override
//    public void handleSendEmailWithTemplate(
//            String recipient, String subject, String templateName,
//            String username, Object object
//    ) {
//        Context context = new Context();
//
//        context.setVariable("name", username);
//        context.setVariable("jobs", object);
//
//        String content = this.templateEngine.process(templateName, context);
//        this.asyncEmailService.handleSendEmailSync(recipient, subject, content, false, true);
//    }

    @Override
    public void handleSendVerificationEmail(String email, String verificationCode) {
        String subject = "Account Verification";
        Context context = new Context();
        context.setVariable("verificationCode", verificationCode);
        String content = this.templateEngine.process("verification", context);

        this.asyncEmailService.handleSendEmailSync(email, subject, content, false, true);
    }

    @Override
    public void handleSendBlogActionNotice(
            String recipient, String username, Object object, String reason, ActionType mode
    ) {
        String subject;

        switch (mode) {
            case ACCEPT -> subject = "Bài viết của bạn đã được duyệt";
            case DELETE -> subject = "Bài viết của bạn đã bị xóa";
            case REJECT -> subject = "Bài viết của bạn không được duyệt";
            default -> subject = "Thông báo về bài viết của bạn";
        }

        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("blogs", object);
        context.setVariable("mode", mode);

        if (mode == ActionType.DELETE || mode == ActionType.REJECT) {
            context.setVariable("reason", reason);
        } else {
            context.setVariable("reason", null);
        }

        String content = this.templateEngine.process("blog", context);
        this.asyncEmailService.handleSendEmailSync(recipient, subject, content, false, true);
    }

    @Override
    public void handleSendReviewActionNotice(String recipient, String username, Object object, String reason, ActionType mode) {
        String subject;

        switch (mode) {
            case ACCEPT -> subject = "Đánh giá của bạn đã được duyệt";
            case DELETE -> subject = "Đánh giá của bạn đã bị xóa";
            case REJECT -> subject = "Đánh giá của bạn không được duyệt";
            case ENABLE -> subject = "Đánh giá của bạn đã được hiển thị";
            case DISABLE -> subject = "Đánh giá của bạn đã bị ẩn";
            default -> subject = "Thông báo về đánh giá của bạn";
        }

        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("reviews", object);
        context.setVariable("mode", mode);

        if (mode == ActionType.DELETE || mode == ActionType.REJECT || mode == ActionType.DISABLE) {
            context.setVariable("reason", reason);
        } else {
            context.setVariable("reason", null);
        }

        String content = this.templateEngine.process("review", context);
        this.asyncEmailService.handleSendEmailSync(recipient, subject, content, false, true);
    }

    @Override
    public void handleSendApplicationEmailToApplicant(String recipient, String fullName, String jobTitle, String companyName) {
        String subject = "Xác nhận ứng tuyển thành công";

        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("jobTitle", jobTitle);
        context.setVariable("companyName", companyName);

        String content = this.templateEngine.process("application/application-applicant", context);
        this.asyncEmailService.handleSendEmailSync(recipient, subject, content, false, true);
    }

    @Override
    public void handleSendApplicationEmailToCompany(String recipient, String name, String jobTitle, String applicantName, String applicantEmail) {
        String subject = "Ứng viên mới cho vị trí " + jobTitle;

        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("jobTitle", jobTitle);
        context.setVariable("applicantName", applicantName);
        context.setVariable("applicantEmail", applicantEmail);


        String content = this.templateEngine.process("application/application-company", context);
        this.asyncEmailService.handleSendEmailSync(recipient, subject, content, false, true);
    }

    @Override
    public void handleSendInterviewEmailToApplicant(String recipient, Object object) {
        String subject = "Thư mời phỏng vấn";

        Context context = new Context();
        context.setVariable("interview", object);

        String content = this.templateEngine.process("interview", context);
        this.asyncEmailService.handleSendEmailSync(recipient, subject, content, false, true);
    }

//    @Override
//    public void handleSendJobActionNotice(String recipient, String username, Object object, String reason, ActionType mode) {
//        String subject;
//
//        switch (mode) {
//            case ACCEPT -> subject = "Việc làm của bạn đã được duyệt";
//            case DELETE -> subject = "Việc làm của bạn đã bị xóa";
//            case REJECT -> subject = "Việc làm của bạn không được duyệt";
//            default -> subject = "Thông báo về việc làm của bạn";
//        }
//
//        Context context = new Context();
//        context.setVariable("username", username);
//        context.setVariable("jobs", object);
//        context.setVariable("mode", mode);
//
//        if (mode == ActionType.DELETE || mode == ActionType.REJECT) {
//            context.setVariable("reason", reason);
//        } else {
//            context.setVariable("reason", "");
//        }
//
//        String content = this.templateEngine.process("job_status", context);
//        this.asyncEmailService.handleSendEmailSync(recipient, subject, content, false, true);
//    }
//
//    @Override
//    public void handleSendApplicationStatusEmail(
//            String recipient, String username, Object object, String status,
//            String interviewType, String interviewDate, String location, String note,
//            String reason
//    ) {
//        String subject = "Thông báo đơn ứng tuyển";
//        Context context = new Context();
//
//        context.setVariable("username", username);
//        context.setVariable("applications", object);
//        context.setVariable("status", status);
//
//        if (Status.ACCEPTED.getValue().equalsIgnoreCase(status)) {
//            context.setVariable("interviewDate", interviewDate);
//            context.setVariable("interviewType", interviewType);
//            context.setVariable("location", location);
//            context.setVariable("note", note);
//        }
//
//        if (Status.REJECTED.getValue().equalsIgnoreCase(status)) {
//            context.setVariable("reason", reason);
//        }
//
//        String content = this.templateEngine.process("applicationStatus", context);
//        this.asyncEmailService.handleSendEmailSync(recipient, subject, content, false, true);
//    }
//
//    @Override
//    public void handleSendJobInvitationEmail(List<Long> applicantIds, Long jobId) {
//        List<Applicant> applicants = this.applicantRepository.findAllById(applicantIds);
//        if(applicants.isEmpty()) return;
//
//        Job job = this.jobRepository.findById(jobId).orElse(null);
//        if(job == null) return;
//
//        String subject = "Thư mời ứng tuyển";
//
//        applicants.forEach(applicant -> {
//            Context context = new Context();
//            context.setVariable("job", job);
//            context.setVariable("applicant", applicant);
//
//            String content = this.templateEngine.process("invitation", context);
//            this.asyncEmailService.handleSendEmailSync(applicant.getContact().getEmail(), subject, content, false, true);
//        });
//    }
//
//    @Override
//    public void handleSendUserEnabledEmail(String recipient, String username, boolean enabled) {
//        String subject = "Xác thực tài khoản";
//
//        Context context = new Context();
//        context.setVariable("username", username);
//        context.setVariable("enabled", enabled);
//
//        String content = this.templateEngine.process("user", context);
//
//        this.asyncEmailService.handleSendEmailSync(recipient, subject, content, false, true);
//    }
}
