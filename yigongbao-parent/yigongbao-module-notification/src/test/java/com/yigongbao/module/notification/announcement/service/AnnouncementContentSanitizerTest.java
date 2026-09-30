package com.yigongbao.module.notification.announcement.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnouncementContentSanitizerTest {

    private final AnnouncementContentSanitizer sanitizer = new AnnouncementContentSanitizer();

    @Test
    void sanitize_removesScriptAndEventHandlers() {
        String html = sanitizer.sanitize("<p onclick=alert(1)>公告</p><script>alert(2)</script>");

        assertTrue(html.contains("公告"));
        assertFalse(html.contains("script"));
        assertFalse(html.contains("onclick"));
    }

    @Test
    void sanitize_removesDangerousProtocolsButKeepsHttpLinks() {
        String html = sanitizer.sanitize("<a href='javascript:alert(1)'>bad</a><a href='https://example.com'>good</a>");

        assertFalse(html.contains("javascript:"));
        assertTrue(html.contains("https://example.com"));
    }

    @Test
    void toPlainText_extractsReadableContent() {
        String text = sanitizer.toPlainText("<h1>标题</h1><p>第一段<br>第二段</p>");

        assertTrue(text.contains("标题"));
        assertTrue(text.contains("第一段"));
        assertTrue(text.contains("第二段"));
    }

    @Test
    void sanitize_keepsInternalRelativeImageUrl() {
        String html = sanitizer.sanitize("<p><img src='/api/files/public/image.png'></p>");

        assertTrue(html.contains("src=\"/api/files/public/image.png\"")
                || html.contains("src='/api/files/public/image.png'"));
    }
}
