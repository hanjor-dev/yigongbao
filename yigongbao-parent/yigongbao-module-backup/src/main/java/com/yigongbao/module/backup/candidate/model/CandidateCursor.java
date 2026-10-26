package com.yigongbao.module.backup.candidate.model;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

/** 按 eligibleAt/eventId 排序的不可变分页游标。 */
public record CandidateCursor(LocalDateTime eligibleAt, Long eventId) {

    public String encode() {
        String raw = eligibleAt + "|" + eventId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static CandidateCursor decode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            int split = raw.lastIndexOf('|');
            if (split <= 0 || split == raw.length() - 1) {
                throw new IllegalArgumentException("游标格式错误");
            }
            return new CandidateCursor(LocalDateTime.parse(raw.substring(0, split)),
                    Long.valueOf(raw.substring(split + 1)));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("游标格式错误", exception);
        }
    }
}
