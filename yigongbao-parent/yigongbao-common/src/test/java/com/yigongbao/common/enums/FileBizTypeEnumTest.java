package com.yigongbao.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FileBizTypeEnumTest {

    @Test
    void drawingQrImage_usesDedicatedDictionaryCode() {
        FileBizTypeEnum type = FileBizTypeEnum.getByDictCode("10.21");

        assertNotNull(type);
        assertEquals(FileBizTypeEnum.DRAWING_QR_IMAGE, type);
        assertEquals("drawing_qr_image", type.getCode());
    }
}
