package com.yigongbao.module.system.test.convert;

import com.yigongbao.module.system.test.dto.CreateTestDTO;
import com.yigongbao.module.system.test.dto.UpdateTestDTO;
import com.yigongbao.module.system.test.entity.TestEntity;
import com.yigongbao.module.system.test.vo.TestVO;

/**
 * 测试数据转换器
 * 用于 Entity、VO、DTO 之间的相互转换
 *
 * @author hanjor
 * @date 2026-03-16
 */
public class TestConvert {

    /**
     * Entity 转换为 VO
     *
     * @param entity 实体
     * @return VO
     */
    public static TestVO toVO(TestEntity entity) {
        if (entity == null) {
            return null;
        }
        TestVO vo = new TestVO();
        vo.setId(entity.getId());
        vo.setKey1(entity.getKey1());
        vo.setValue1(entity.getValue1());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setCreateBy(entity.getCreateBy());
        vo.setUpdateBy(entity.getUpdateBy());
        return vo;
    }

    /**
     * DTO 转换为 Entity
     *
     * @param dto DTO
     * @return 实体
     */
    public static TestEntity toEntity(CreateTestDTO dto) {
        if (dto == null) {
            return null;
        }
        TestEntity entity = new TestEntity();
        entity.setKey1(dto.getKey());
        entity.setValue1(dto.getValue());
        return entity;
    }

    /**
     * DTO 转换为 Entity（更新用）
     *
     * @param dto DTO
     * @return 实体
     */
    public static TestEntity toEntity(UpdateTestDTO dto) {
        if (dto == null) {
            return null;
        }
        TestEntity entity = new TestEntity();
        entity.setKey1(dto.getKey());
        entity.setValue1(dto.getValue());
        return entity;
    }
}
