package com.yigongbao.module.order.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates the customer-facing order identifier.
 */
@Component
public class PublicOrderCodeGenerator {

    private static final String PREFIX = "YG";
    private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int RANDOM_LENGTH = 10;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder code = new StringBuilder(PREFIX);
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
