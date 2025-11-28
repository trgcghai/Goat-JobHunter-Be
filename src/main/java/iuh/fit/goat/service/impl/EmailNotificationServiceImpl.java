package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.BlogActionType;
import iuh.fit.goat.common.Status;
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
    private final ApplicantRepository applicantRepository;
    private final JobRepository jobRepository;


    @Override
    public void handleSendEmailWithTemplate(
            String recipient, String subject, String templateName,
            String username, Object object
    ) {
        Context context = new Context();

        context.setVariable("name", username);
        context.setVariable("jobs", object);

        String content = this.templateEngine.process(templateName, context);
        this.asyncEmailService.handleSendEmailSync(recipient, subject, content, false, true);
    }

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
            String recipient, String username, Object object, String reason, BlogActionType mode
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

        if (mode == BlogActionType.DELETE || mode == BlogActionType.REJECT) {
            context.setVariable("reason", reason);
        } else {
            context.setVariable("reason", null);
        }

        String content = this.templateEngine.process("blog", context);
        this.asyncEmailService.handleSendEmailSync("nguyenthangdat84@gmail.com", subject, content, false, true);
    }

    @Override
    public void handleSendApplicationStatusEmail(
            String recipient, String username, Object object, String status,
            String interviewType, String interviewDate, String location, String note,
            String reason
    ) {
        String subject = "Thông báo đơn ứng tuyển";
        Context context = new Context();

        context.setVariable("username", username);
        context.setVariable("applications", object);
        context.setVariable("status", status);

        if (Status.ACCEPTED.getValue().equalsIgnoreCase(status)) {
            context.setVariable("interviewDate", interviewDate);
            context.setVariable("interviewType", interviewType);
            context.setVariable("location", location);
            context.setVariable("note", note);
        }

        if (Status.REJECTED.getValue().equalsIgnoreCase(status)) {
            context.setVariable("reason", reason);
        }

        String content = this.templateEngine.process("application", context);
        this.asyncEmailService.handleSendEmailSync(recipient, subject, content, false, true);
    }

    @Override
    public void handleSendJobInvitationEmail(List<Long> applicantIds, Long jobId) {
        List<Applicant> applicants = this.applicantRepository.findAllById(applicantIds);
        if(applicants.isEmpty()) return;

        Job job = this.jobRepository.findById(jobId).orElse(null);
        if(job == null) return;

        String subject = "Thư mời ứng tuyển";

        applicants.forEach(applicant -> {
            Context context = new Context();
            context.setVariable("job", job);
            context.setVariable("applicant", applicant);

            String content = this.templateEngine.process("invitation", context);
            this.asyncEmailService.handleSendEmailSync(applicant.getContact().getEmail(), subject, content, false, true);
        });
    }
}
