package com.yigongbao.module.order.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicOrderCodeGeneratorTest {

    @Test
    void generate_returnsTwelveCharactersWithEightDigitsAndFourLetters() {
        PublicOrderCodeGenerator generator = new PublicOrderCodeGenerator();

        String code = generator.generate();

        assertEquals(12, code.length());
        assertTrue(code.matches("[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{12}"));
        assertEquals(8, code.replaceAll("[^23456789]", "").length());
        assertEquals(4, code.replaceAll("[^ABCDEFGHJKMNPQRSTUVWXYZ]", "").length());
    }

    @Test
    void generate_returnsDifferentCodesForRepeatedCalls() {
        PublicOrderCodeGenerator generator = new PublicOrderCodeGenerator();
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            codes.add(generator.generate());
        }

        assertEquals(100, codes.size());
    }
}
