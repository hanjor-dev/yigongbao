package com.yigongbao.module.production.record.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 打印机相对当前流转卡的占用快照。 */
@Getter
@AllArgsConstructor
public class PrinterOccupationVO {

    private final Boolean occupied;
}
