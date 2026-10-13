package com.yigongbao.module.order.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates the customer-facing order identifier.
 */
@Component
public class PublicOrderCodeGenerator {

    private static final String DIGITS = "23456789";
    private static final String LETTERS = "ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int DIGIT_COUNT = 8;
    private static final int LETTER_COUNT = 4;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        char[] code = new char[DIGIT_COUNT + LETTER_COUNT];
        int index = 0;
        for (int i = 0; i < DIGIT_COUNT; i++) {
            code[index++] = DIGITS.charAt(random.nextInt(DIGITS.length()));
        }
        for (int i = 0; i < LETTER_COUNT; i++) {
            code[index++] = LETTERS.charAt(random.nextInt(LETTERS.length()));
        }

        for (int i = code.length - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            char current = code[i];
            code[i] = code[swapIndex];
            code[swapIndex] = current;
        }
        return new String(code);
    }
}
