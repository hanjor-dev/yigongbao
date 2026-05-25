package com.yigongbao.module.basic.device.convert;

import com.yigongbao.module.basic.device.dto.CreateDeviceDTO;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.vo.DeviceVO;
import org.springframework.beans.BeanUtils;

public class DeviceConvert {

    public static DeviceEntity toEntity(CreateDeviceDTO dto) {
        DeviceEntity entity = new DeviceEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    public static DeviceVO toVO(DeviceEntity entity) {
        DeviceVO vo = new DeviceVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
