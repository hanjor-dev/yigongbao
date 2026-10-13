package com.yigongbao.module.design.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DesignDrawingEntityTest {

    @Test
    void storesQrFileIdSnapshot() {
        DesignDrawingEntity entity = new DesignDrawingEntity();
        entity.setQrFileId("qr-file-001");

        assertEquals("qr-file-001", entity.getQrFileId());
    }
}
