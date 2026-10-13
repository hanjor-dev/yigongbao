package com.yigongbao.module.basic.processingCenter.convert;

import com.yigongbao.module.basic.processingCenter.dto.CreateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.vo.ProcessingCenterVO;
import org.springframework.beans.BeanUtils;

public class ProcessingCenterConvert {

    public static ProcessingCenterEntity toEntity(CreateProcessingCenterDTO dto) {
        ProcessingCenterEntity entity = new ProcessingCenterEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    public static ProcessingCenterVO toVO(ProcessingCenterEntity entity) {
        ProcessingCenterVO vo = new ProcessingCenterVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
