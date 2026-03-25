package com.yigongbao.module.basic.rebuildProject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.rebuildProject.dto.CreateRebuildProjectDTO;
import com.yigongbao.module.basic.rebuildProject.dto.UpdateRebuildProjectDTO;
import com.yigongbao.module.basic.rebuildProject.entity.RebuildProjectEntity;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectDetailVO;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectOptionVO;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectVO;

import java.util.List;

/**
 * 重建项目 Service 接口
 *
 * @author hanjor
 * @date 2026-03-23
 */
public interface RebuildProjectService extends IService<RebuildProjectEntity> {

    /**
     * 获取项目树形结构（按部位分组）
     *
     * @param category 项目分类（可选，不传则返回全部）
     * @return 项目树形列表
     */
    List<RebuildProjectVO> listTree(String category);

    /**
     * 根据部位ID获取项目列表
     *
     * @param bodyPartId 部位ID
     * @param category   项目分类（可选，不传则返回全部）
     * @return 该部位下的项目树
     */
    List<RebuildProjectVO> listByBodyPartId(Long bodyPartId, String category);

    /**
     * 获取项目下拉选项
     *
     * @param bodyPartId 部位ID（可选，不传则返回全部）
     * @param category   项目分类（可选，不传则返回全部）
     * @return 项目下拉选项列表
     */
    List<RebuildProjectOptionVO> listOptions(Long bodyPartId, String category);

    /**
     * 查询项目详情
     *
     * @param id 项目ID
     * @return 项目详情
     */
    RebuildProjectDetailVO getDetailById(Long id);

    /**
     * 创建项目
     *
     * @param dto 创建参数
     */
    void createProject(CreateRebuildProjectDTO dto);

    /**
     * 更新项目
     *
     * @param id  项目ID
     * @param dto 更新参数
     */
    void updateProject(Long id, UpdateRebuildProjectDTO dto);

    /**
     * 删除项目
     *
     * @param id 项目ID
     */
    void removeProject(Long id);

    /**
     * 修改项目状态
     *
     * @param id     项目ID
     * @param status 状态（0=禁用，1=正常）
     */
    void updateStatus(Long id, Integer status);
}
