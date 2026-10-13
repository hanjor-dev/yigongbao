package com.yigongbao.module.system.dept.convert;

import com.yigongbao.module.system.dept.dto.CreateDeptDTO;
import com.yigongbao.module.system.dept.dto.UpdateDeptDTO;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import com.yigongbao.module.system.dept.vo.DeptVO;
import org.springframework.beans.BeanUtils;

/**
 * 部门转换器，负责 Entity、VO、DTO 之间的对象转换
 *
 * @author hanjor
 * @date 2026-03-17
 */
public class DeptConvert {

    /**
     * 将部门实体转换为视图对象
     *
     * @param entity 部门实体
     * @return 部门视图对象，entity 为 null 时返回 null
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
     * 将创建部门 DTO 转换为实体，排除 orgIds 嵌套集合字段
     *
     * @param dto 创建部门DTO
     * @return 部门实体，dto 为 null 时返回 null
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
     * 将更新部门 DTO 转换为实体，排除 orgIds 嵌套集合字段
     *
     * @param dto 更新部门DTO
     * @return 部门实体，dto 为 null 时返回 null
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
