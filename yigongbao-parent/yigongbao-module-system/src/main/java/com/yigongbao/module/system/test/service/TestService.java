package com.yigongbao.module.system.test.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.system.test.dto.CreateTestDTO;
import com.yigongbao.module.system.test.dto.UpdateTestDTO;
import com.yigongbao.module.system.test.entity.TestEntity;
import com.yigongbao.module.system.test.vo.TestVO;

import java.util.List;

/**
 * 测试 Service
 *
 * @author hanjor
 * @date 2026-03-14 18:25:00
 */
public interface TestService extends IService<TestEntity> {

    /**
     * 查询所有测试数据
     *
     * @return 测试数据列表
     */
    List<TestVO> listVo();

    /**
     * 根据ID查询测试数据
     *
     * @param id 主键ID
     * @return 测试数据
     */
    TestVO getVoById(Long id);

    /**
     * 创建测试数据
     *
     * @param dto 创建参数
     */
    void create(CreateTestDTO dto);

    /**
     * 更新测试数据
     *
     * @param id  主键ID
     * @param dto 更新参数
     */
    void update(Long id, UpdateTestDTO dto);

    /**
     * 删除测试数据
     *
     * @param id 主键ID
     */
    void remove(Long id);
}
