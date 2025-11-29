package com.daspawnw.sammelalbum.service.email;

import com.daspawnw.sammelalbum.model.EmailOutbox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(name = "app.mail.sender", havingValue = "console", matchIfMissing = true)
public class ConsoleEmailSender implements EmailSender {

    @Override
    public void send(EmailOutbox email) {
        log.info(">>> SENDING EMAIL (Console) >>>");
        log.info("To: {}", email.getRecipientEmail());
        log.info("Subject: {}", email.getSubject());
        log.info("Body: \n{}", email.getBody());
        log.info("<<< EMAIL SENT (Console) <<<");
    }
}
