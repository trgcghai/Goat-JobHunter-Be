package iuh.fit.goat.service.impl;

import iuh.fit.goat.entity.User;
import iuh.fit.goat.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final MailSender mailSender;
    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    //    Send email with text
    @Override
    public void handleSendEmail(){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("nguyenthangdat84@gmail.com");
        message.setSubject("Hello World");
        message.setText("Hello");

        this.mailSender.send(message);    }

    //    Send email with text and html
    @Override
    public void handleSendEmailSync(String recipient, String subject, String content,
                                boolean isMultipart, boolean isHtml){
        MimeMessage mimeMessage = this.javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(content, isHtml);

            this.javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            System.out.println("Error in sending email");
        }
    }

    //    Send email with template
    @Async
    @Override
    public void handleSendEmailWithTemplate(String recipient, String subject, String templateName,
                                        String username, Object object){
        Context context = new Context();

        context.setVariable("name", username);
        context.setVariable("jobs", object);

        String content = this.templateEngine.process(templateName, context);
        this.handleSendEmailSync(recipient, subject, content, false, true);
    }

    @Override
    public void handleSendVerificationEmail(User user) {
        String subject = "Account Verification";
        Context context = new Context();
        context.setVariable("verificationCode", user.getVerificationCode());
        String content = this.templateEngine.process("verification", context);

        this.handleSendEmailSync(user.getContact().getEmail(), subject, content, false, true);
    }
}
