package com.daspawnw.sammelalbum.service.email;

import com.daspawnw.sammelalbum.model.EmailOutbox;

public interface EmailSender {
    void send(EmailOutbox email);
}
