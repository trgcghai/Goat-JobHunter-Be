package iuh.fit.goat.service.impl;

import iuh.fit.goat.service.AsyncEmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AsyncEmailServiceImpl implements AsyncEmailService {
    private final MailSender mailSender;
    private final JavaMailSender javaMailSender;

    @Override
    public void handleSendEmail() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("nguyenthangdat84@gmail.com");
        message.setSubject("Hello World");
        message.setText("Hello");

        this.mailSender.send(message);
    }

    @Override
    @Async("emailExecutor")
    public void handleSendEmailSync(String recipient, String subject, String content, boolean isMultipart, boolean isHtml)
    {
        MimeMessage mimeMessage = this.javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(content, isHtml);

            this.javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            System.out.println("Error sending email to " + recipient + ": " + e.getMessage());
        }
    }
}
