package com.yigongbao.module.basic.device.convert;

import com.yigongbao.module.basic.device.entity.DeviceStateLogEntity;
import com.yigongbao.module.basic.device.vo.DeviceStateLogVO;
import org.springframework.beans.BeanUtils;

public class DeviceStateLogConvert {

    public static DeviceStateLogVO toVO(DeviceStateLogEntity entity) {
        DeviceStateLogVO vo = new DeviceStateLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
