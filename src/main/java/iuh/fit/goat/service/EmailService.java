package iuh.fit.goat.service;

import iuh.fit.goat.entity.User;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDate;
import java.util.List;

public interface EmailService {
    //    Send email with text
    void handleSendEmail();

    //    Send email with text and html
    void handleSendEmailSync(String recipient, String subject, String content,
                             boolean isMultipart, boolean isHtml);

    //    Send email with template
    @Async
    void handleSendEmailWithTemplate(
            String recipient, String subject, String templateName,
         String username, Object object
    );

    void handleSendVerificationEmail(String email, String verificationCode);

    void handleSendBlogActionNotice(
            String recipient, String username,
            Object object, String reason, String mode
    );

    void handelSendApplicationStatusEmail(
            String recipient, String username, Object object, String status,
            String interviewType, String interviewDate, String location, String note,
            String reason
    );

    void handelSendJobInvitationEmail(List<Long> applicantIds, Long jobId);
}
