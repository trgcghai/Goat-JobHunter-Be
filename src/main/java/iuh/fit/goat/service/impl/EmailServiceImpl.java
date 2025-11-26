package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.BlogActionType;
import iuh.fit.goat.common.Status;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.repository.ApplicantRepository;
import iuh.fit.goat.repository.JobRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final MailSender mailSender;
    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;
    private final ApplicantRepository applicantRepository;
    private final JobRepository jobRepository;

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
    public void handleSendVerificationEmail(String email, String verificationCode) {
        String subject = "Account Verification";
        Context context = new Context();
        context.setVariable("verificationCode", verificationCode);
        String content = this.templateEngine.process("verification", context);

        this.handleSendEmailSync(email, subject, content, false, true);
    }

    @Override
    public void handleSendBlogActionNotice(
            String recipient, String username, Object object, String reason, String mode
    ) {
        String subject = mode.equalsIgnoreCase(BlogActionType.DELETE.getValue())
                ? "Bài viết của bạn đã bị xóa" : "Bài viết của bạn không được duyệt";

        Context context = new Context();

        context.setVariable("name", username);
        context.setVariable("blogs", object);
        context.setVariable("reason", reason);
        context.setVariable("mode", mode);

        String content = this.templateEngine.process("blog", context);
        this.handleSendEmailSync(recipient, subject, content, false, true);
    }

    @Override
    public void handelSendApplicationStatusEmail(
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
        this.handleSendEmailSync(recipient, subject, content, false, true);
    }

    @Override
    public void handelSendJobInvitationEmail(List<Long> applicantIds, Long jobId) {
        List<Applicant> applicants = this.applicantRepository.findAllById(applicantIds);
        if(applicants.isEmpty()) return;

        Job job = this.jobRepository.findById(jobId).orElse(null);
        if(job == null) return;

        String subject = "Thư mời ứng tuyển";
        Context context = new Context();
        context.setVariable("job", job);

        applicants.forEach(applicant -> {
            context.setVariable("applicant", applicant);
            String content = this.templateEngine.process("invitation", context);
            this.handleSendEmailSync(applicant.getContact().getEmail(), subject, content, false, true);
        });

    }
}
