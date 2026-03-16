package com.yigongbao.module.system.test.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.test.convert.TestConvert;
import com.yigongbao.module.system.test.dto.CreateTestDTO;
import com.yigongbao.module.system.test.dto.UpdateTestDTO;
import com.yigongbao.module.system.test.entity.TestEntity;
import com.yigongbao.module.system.test.mapper.TestMapper;
import com.yigongbao.module.system.test.service.TestService;
import com.yigongbao.module.system.test.vo.TestVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 测试 Service 实现类
 * 提供测试数据相关的业务逻辑处理，包括CRUD操作
 *
 * @author hanjor
 * @date 2026-03-14 18:25:00
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestServiceImpl extends ServiceImpl<TestMapper, TestEntity> implements TestService {

    /**
     * 查询所有测试数据
     *
     * @return 测试数据列表
     */
    @Override
    public List<TestVO> listVo() {
        // 查询所有未删除的数据
        List<TestEntity> list = list();
        // 转换为VO列表
        return list.stream()
                .map(TestConvert::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID查询测试数据
     *
     * @param id 主键ID
     * @return 测试数据
     * @throws BusinessException 数据不存在
     */
    @Override
    public TestVO getVoById(Long id) {
        // 记录查询入参
        log.info("根据ID查询测试数据，id={}", id);
        try {
            // 根据ID查询实体
            TestEntity entity = getById(id);
            // 校验数据是否存在
            if (entity == null) {
                log.warn("测试数据不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            // 转换为VO返回
            TestVO vo = TestConvert.toVO(entity);
            log.info("查询测试数据成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询测试数据异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 创建测试数据
     *
     * @param dto 创建参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CreateTestDTO dto) {
        // 记录创建入参
        log.info("创建测试数据，key={}", dto.getKey());
        try {
            // DTO转换为实体对象
            TestEntity entity = TestConvert.toEntity(dto);
            // 插入数据库
            save(entity);
            // 记录创建成功
            log.info("创建测试数据成功，id={}, key={}", entity.getId(), dto.getKey());
        } catch (Exception e) {
            log.error("创建测试数据异常，key={}", dto.getKey(), e);
            throw e;
        }
    }

    /**
     * 更新测试数据
     *
     * @param id  主键ID
     * @param dto 更新参数
     * @throws BusinessException 数据不存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UpdateTestDTO dto) {
        // 记录更新入参
        log.info("更新测试数据，id={}, key={}", id, dto.getKey());
        try {
            // 根据ID查询实体
            TestEntity entity = getById(id);
            // 校验数据是否存在
            if (entity == null) {
                log.warn("测试数据不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            // 更新数据
            entity.setKey1(dto.getKey());
            entity.setValue1(dto.getValue());
            // 更新数据库
            updateById(entity);
            // 记录更新成功
            log.info("更新测试数据成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新测试数据异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 删除测试数据
     *
     * @param id 主键ID
     * @throws BusinessException 数据不存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        // 记录删除入参
        log.info("删除测试数据，id={}", id);
        try {
            // 校验数据是否存在并删除
            if (!removeById(id)) {
                log.warn("测试数据不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            // 记录删除成功
            log.info("删除测试数据成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除测试数据异常，id={}", id, e);
            throw e;
        }
    }
}
