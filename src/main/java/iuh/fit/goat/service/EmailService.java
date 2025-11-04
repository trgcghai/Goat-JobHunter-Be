package iuh.fit.goat.service;

import iuh.fit.goat.entity.User;
import org.springframework.scheduling.annotation.Async;

public interface EmailService {
    //    Send email with text
    void handleSendEmail();

    //    Send email with text and html
    void handleSendEmailSync(String recipient, String subject, String content,
                             boolean isMultipart, boolean isHtml);

    //    Send email with template
    @Async
    void handleSendEmailWithTemplate(String recipient, String subject, String templateName,
                                     String username, Object object);

    void handleSendVerificationEmail(User user);
}
