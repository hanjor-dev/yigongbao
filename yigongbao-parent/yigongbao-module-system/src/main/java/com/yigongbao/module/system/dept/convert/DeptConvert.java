package com.yigongbao.module.system.dept.convert;

import com.yigongbao.module.system.dept.dto.CreateDeptDTO;
import com.yigongbao.module.system.dept.dto.UpdateDeptDTO;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import com.yigongbao.module.system.dept.vo.DeptVO;
import org.springframework.beans.BeanUtils;

/**
 * 部门转换器
 * 用于 Entity/VO/DTO 之间的转换
 *
 * @author hanjor
 * @date 2026-03-17
 */
public class DeptConvert {

    /**
     * Entity 转 VO
     *
     * @param entity 部门实体
     * @return 部门视图对象
     */
    public static DeptVO toVO(DeptEntity entity) {
        if (entity == null) {
            return null;
        }
        DeptVO vo = new DeptVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * DTO 转 Entity
     *
     * @param dto 创建部门DTO
     * @return 部门实体
     */
    public static DeptEntity toEntity(CreateDeptDTO dto) {
        if (dto == null) {
            return null;
        }
        DeptEntity entity = new DeptEntity();
        BeanUtils.copyProperties(dto, entity, "orgIds");
        return entity;
    }

    /**
     * DTO 转 Entity（更新）
     *
     * @param dto 更新部门DTO
     * @return 部门实体
     */
    public static DeptEntity toEntity(UpdateDeptDTO dto) {
        if (dto == null) {
            return null;
        }
        DeptEntity entity = new DeptEntity();
        BeanUtils.copyProperties(dto, entity, "orgIds");
        return entity;
    }
}
