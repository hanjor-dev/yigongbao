package com.yigongbao.module.system.basedata.area.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.system.basedata.area.entity.AreaEntity;
import com.yigongbao.module.system.basedata.area.vo.AreaVO;

import java.util.List;

/**
 * 地区 Service 接口
 *
 * @author hanjor
 * @date 2026-03-17
 */
public interface AreaService extends IService<AreaEntity> {

    /**
     * 获取地区树形结构
     *
     * @param parentId 父级ID（null时查询完整树）
     * @return 树形结构列表
     */
    List<AreaVO> listTree(Long parentId);

    /**
     * 根据父级ID查询子地区列表
     *
     * @param parentId 父级ID
     * @return 子地区列表
     */
    List<AreaVO> listByParentId(Long parentId);

    /**
     * 获取省份列表（parentId=0）
     *
     * @return 省份列表
     */
    List<AreaVO> listProvinces();
}
