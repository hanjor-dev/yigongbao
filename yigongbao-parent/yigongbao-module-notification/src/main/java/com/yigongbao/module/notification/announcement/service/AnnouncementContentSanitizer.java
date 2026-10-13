package com.yigongbao.module.notification.announcement.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 使用白名单清洗公告富文本，预览和发布必须共用此组件。 */
@Component
public class AnnouncementContentSanitizer {

    private static final String RELATIVE_URL_PREFIX = "https://announcement-relative.invalid";

    private final Safelist safelist = Safelist.relaxed()
            .addTags("table", "thead", "tbody", "tfoot", "tr", "th", "td")
            .addAttributes("a", "target")
            .addProtocols("a", "href", "http", "https")
            .addProtocols("img", "src", "http", "https");

    public String sanitize(String html) {
        if (!StringUtils.hasText(html)) {
            throw new IllegalArgumentException("公告内容不能为空");
        }
        String normalized = html.replaceAll("(?i)(\\b(?:href|src)\\s*=\\s*[\\\"'])(/(?!/)[^\\\"']*)([\\\"'])",
                "$1" + RELATIVE_URL_PREFIX + "$2$3");
        String cleaned = Jsoup.clean(normalized, "", safelist, new Document.OutputSettings().prettyPrint(false));
        return cleaned.replace(RELATIVE_URL_PREFIX, "");
    }

    public String toPlainText(String sanitizedHtml) {
        return Jsoup.parse(sanitizedHtml).text();
    }
}
