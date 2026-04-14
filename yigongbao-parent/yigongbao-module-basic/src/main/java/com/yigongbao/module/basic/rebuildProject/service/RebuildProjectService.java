package com.yigongbao.module.basic.rebuildProject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.rebuildProject.dto.CreateRebuildProjectDTO;
import com.yigongbao.module.basic.rebuildProject.dto.UpdateRebuildProjectDTO;
import com.yigongbao.module.basic.rebuildProject.entity.RebuildProjectEntity;
import com.yigongbao.module.basic.rebuildProject.vo.BodyPartProjectTreeVO;
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
     * @param categoryCode 项目分类编码（字典 dict_code=13，可选，传入则精确匹配）
     * @return 项目树形列表
     */
    List<RebuildProjectVO> listTree(String categoryCode);

    /**
     * 根据部位ID获取项目列表
     *
     * @param bodyPartId   部位ID
     * @param categoryCode 项目分类编码（字典 dict_code=13，可选，不传则返回全部）
     * @return 该部位下的项目树
     */
    List<RebuildProjectVO> listByBodyPartId(Long bodyPartId, String categoryCode);

    /**
     * 获取项目下拉选项
     *
     * @param bodyPartId   部位ID（可选，不传则返回全部）
     * @param categoryCode 项目分类编码（字典 dict_code=13，可选，不传则返回全部）
     * @return 项目下拉选项列表
     */
    List<RebuildProjectOptionVO> listOptions(Long bodyPartId, String categoryCode);

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

    /**
     * 获取完整部位-项目树形结构
     * 返回三层结构：部位 → 重建项目 → 子重建项目
     * 仅包含启用状态的部位和项目
     *
     * @param categoryCode 项目分类编码（可选，传入则精确匹配，不传则返回全部）
     * @return 按部位分组的项目树列表
     */
    List<BodyPartProjectTreeVO> listFullTree(String categoryCode);

    /**
     * 根据项目ID查询专业方向字典编码
     * 供 order 模块的设计师分配逻辑使用
     *
     * @param projectId 重建项目ID
     * @return 专业方向字典编码（如 "7.1"），项目不存在或未设置时返回 null
     */
    String getSpecialtyByProjectId(Long projectId);
}
