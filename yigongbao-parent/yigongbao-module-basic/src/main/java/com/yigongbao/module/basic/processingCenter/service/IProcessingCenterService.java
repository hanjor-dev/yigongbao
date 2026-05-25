package com.yigongbao.module.basic.processingCenter.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.processingCenter.dto.CreateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.dto.ProcessingCenterPageDTO;
import com.yigongbao.module.basic.processingCenter.dto.UpdateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.vo.ProcessingCenterVO;
import java.util.List;

public interface IProcessingCenterService extends IService<ProcessingCenterEntity> {
    IPage<ProcessingCenterVO> listProcessingCenters(ProcessingCenterPageDTO dto);
    ProcessingCenterVO getProcessingCenterById(Long id);
    Long createProcessingCenter(CreateProcessingCenterDTO dto);
    void updateProcessingCenter(UpdateProcessingCenterDTO dto);
    void deleteProcessingCenter(Long id);
    List<ProcessingCenterVO> listAllEnabled();
}
