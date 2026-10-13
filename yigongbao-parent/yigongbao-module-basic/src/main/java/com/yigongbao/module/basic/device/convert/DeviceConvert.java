package com.yigongbao.module.basic.device.convert;

import com.yigongbao.module.basic.device.dto.CreateDeviceDTO;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.enums.DeviceTypeEnum;
import com.yigongbao.module.basic.device.vo.DeviceVO;
import com.yigongbao.common.enums.PrinterDeviceStateEnum;
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
        if (DeviceTypeEnum.PRINTER_SLA.getCode().equals(entity.getDeviceType())) {
            PrinterDeviceStateEnum state = PrinterDeviceStateEnum.fromCode(entity.getState());
            if (state != null) {
                vo.setStateName(state.getName());
            }
        } else if (Integer.valueOf(0).equals(entity.getState())) {
            vo.setStateName("空闲");
        } else if (Integer.valueOf(1).equals(entity.getState())) {
            vo.setStateName("占用");
        }
        return vo;
    }
}
