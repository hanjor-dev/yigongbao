package com.yigongbao.module.basic.operationlog.convert;

import com.yigongbao.module.basic.operationlog.dto.OperationLogQueryDTO;
import com.yigongbao.module.basic.operationlog.entity.OperationLogEntity;
import com.yigongbao.module.basic.operationlog.vo.OperationLogVO;
import org.springframework.beans.BeanUtils;

/**
 * 操作日志转换器
 * 用于 Entity/VO/DTO 之间的转换
 *
 * @author hanjor
 * @date 2026-03-24
 */
public class OperationLogConvert {

    /**
     * Entity 转 VO
     */
    public static OperationLogVO toVO(OperationLogEntity entity) {
        if (entity == null) {
            return null;
        }
        OperationLogVO vo = new OperationLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
