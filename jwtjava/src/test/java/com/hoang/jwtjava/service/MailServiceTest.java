package com.hoang.jwtjava.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MailServiceTest {

    @Test
    void parseSender_withDisplayName() {
        MailService.Sender sender = MailService.parseSender("Easy Mart <you@gmail.com>");
        assertEquals("Easy Mart", sender.name());
        assertEquals("you@gmail.com", sender.email());
    }

    @Test
    void parseSender_emailOnly() {
        MailService.Sender sender = MailService.parseSender("you@gmail.com");
        assertEquals("you@gmail.com", sender.name());
        assertEquals("you@gmail.com", sender.email());
    }

    @Test
    void parseSender_blank() {
        assertNull(MailService.parseSender(" "));
        assertNull(MailService.parseSender(null));
    }
}
