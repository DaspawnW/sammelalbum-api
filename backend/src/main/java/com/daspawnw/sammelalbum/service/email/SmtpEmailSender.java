package com.daspawnw.sammelalbum.service.email;

import com.daspawnw.sammelalbum.model.EmailOutbox;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.sender", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;

    @org.springframework.beans.factory.annotation.Value("${app.mail.from:noreply@sammelalbum.com}")
    private String fromAddress;

    @Override
    public void send(EmailOutbox email) {
        try {
            log.info("Attempting to send email via SMTP to: {}", email.getRecipientEmail());

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            helper.setTo(email.getRecipientEmail());
            helper.setSubject(email.getSubject());
            helper.setText(email.getBody(), true); // true = isHtml
            helper.setFrom(fromAddress);

            javaMailSender.send(message);

            log.info("Email sent successfully via SMTP to: {}", email.getRecipientEmail());
        } catch (MessagingException e) {
            log.error("Failed to send email via SMTP to: {}", email.getRecipientEmail(), e);
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending email via SMTP to: {}", email.getRecipientEmail(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
