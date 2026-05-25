package com.yigongbao.module.basic.processingCenter.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ProcessingCenterPageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String centerName;
    private Integer status;
}
