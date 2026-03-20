package com.yigongbao.module.basic.area.service.impl;

/**
 * 地区 Service 实现类
 * 提供省/市/区三级行政区划数据的查询能力
 *
 * @author hanjor
 * @date 2026-03-19
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.basic.area.convert.AreaConvert;
import com.yigongbao.module.basic.area.entity.AreaEntity;
import com.yigongbao.module.basic.area.mapper.AreaMapper;
import com.yigongbao.module.basic.area.service.AreaService;
import com.yigongbao.module.basic.area.vo.AreaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 地区 Service 实现类
 * 处理地区相关的业务逻辑，包括省市区三级联动查询等
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class AreaServiceImpl extends ServiceImpl<AreaMapper, AreaEntity> implements AreaService {

    /** 根节点父级行政代码（省/直辖市） */
    private static final Long ROOT_PARENT_CODE = 0L;

    /**
     * 获取地区树形结构
     *
     * @param parentCode 父级行政代码（null 时从根 0 开始）
     * @return 树形结构列表
     */
    @Override
    public List<AreaVO> listTree(Long parentCode) {
        log.info("获取地区树形结构，parentCode={}", parentCode);
        try {
            List<AreaEntity> allList = list(new LambdaQueryWrapper<AreaEntity>()
                    .orderByAsc(AreaEntity::getAreaCode));
            Long rootCode = parentCode != null ? parentCode : ROOT_PARENT_CODE;
            List<AreaVO> tree = buildTree(allList, rootCode);
            log.info("获取地区树形结构成功，根节点数量={}", tree.size());
            return tree;
        } catch (Exception e) {
            log.error("获取地区树形结构异常，parentCode={}", parentCode, e);
            throw e;
        }
    }

    /**
     * 根据父级行政代码查询子地区列表
     *
     * @param parentCode 父级行政代码
     * @return 子地区列表
     */
    @Override
    public List<AreaVO> listByParentId(Long parentCode) {
        log.info("根据父级行政代码查询子地区列表，parentCode={}", parentCode);
        try {
            Long pid = parentCode != null ? parentCode : ROOT_PARENT_CODE;
            List<AreaEntity> list = list(new LambdaQueryWrapper<AreaEntity>()
                    .eq(AreaEntity::getParentCode, pid)
                    .orderByAsc(AreaEntity::getAreaCode));
            log.info("查询子地区列表成功，数量={}", list.size());
            return AreaConvert.toVOList(list);
        } catch (Exception e) {
            log.error("根据父级行政代码查询子地区列表异常，parentCode={}", parentCode, e);
            throw e;
        }
    }

    /**
     * 获取省份列表（parent_code=0）
     *
     * @return 省份列表
     */
    @Override
    public List<AreaVO> listProvinces() {
        log.info("获取省份列表");
        try {
            List<AreaEntity> list = list(new LambdaQueryWrapper<AreaEntity>()
                    .eq(AreaEntity::getParentCode, ROOT_PARENT_CODE)
                    .orderByAsc(AreaEntity::getAreaCode));
            log.info("获取省份列表成功，数量={}", list.size());
            return AreaConvert.toVOList(list);
        } catch (Exception e) {
            log.error("获取省份列表异常", e);
            throw e;
        }
    }

    /**
     * 递归构建树形结构（按 parent_code / area_code）
     *
     * @param allList 所有地区数据
     * @param parentCode 父级行政代码
     * @return 树形列表
     */
    private List<AreaVO> buildTree(List<AreaEntity> allList, Long parentCode) {
        return allList.stream()
                .filter(e -> Objects.equals(e.getParentCode(), parentCode))
                .map(e -> {
                    AreaVO vo = AreaConvert.toVO(e);
                    List<AreaVO> children = buildTree(allList, e.getAreaCode());
                    if (children != null && !children.isEmpty()) {
                        vo.setChildren(children);
                    }
                    return vo;
                })
                .sorted(Comparator.comparing(AreaVO::getAreaCode, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }
}
