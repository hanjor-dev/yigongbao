package com.yigongbao.module.system.dept.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.system.dept.dto.CreateDeptDTO;
import com.yigongbao.module.system.dept.dto.DeptPageDTO;
import com.yigongbao.module.system.dept.dto.DeptStatisticsQueryDTO;
import com.yigongbao.module.system.dept.dto.UpdateDeptDTO;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import com.yigongbao.module.system.dept.vo.DeptVO;
import com.yigongbao.module.system.dept.vo.DeptStatisticsVO;

import java.util.List;

/**
 * 部门 Service 接口，定义部门管理相关业务操作
 *
 * @author hanjor
 * @date 2026-03-17
 */
public interface DeptService extends IService<DeptEntity> {
    DeptStatisticsVO getStatistics(DeptStatisticsQueryDTO dto);

    /**
     * 分页查询部门列表
     *
     * @param dto 分页查询参数
     * @return 分页后的部门列表
     */
    IPage<DeptVO> listDept(DeptPageDTO dto);

    /**
     * 根据ID查询部门详情
     *
     * @param id 部门ID
     * @return 部门详情
     */
    DeptVO getDeptById(Long id);

    /**
     * 创建部门
     *
     * @param dto 创建参数
     */
    void createDept(CreateDeptDTO dto);

    /**
     * 更新部门
     *
     * @param id  部门ID
     * @param dto 更新参数
     */
    void updateDept(Long id, UpdateDeptDTO dto);

    /**
     * 删除部门
     *
     * @param id 部门ID
     */
    void removeDept(Long id);

    /**
     * 修改部门状态
     *
     * @param id     部门ID
     * @param status 状态（0=禁用，1=正常）
     */
    void updateStatus(Long id, Integer status);

    /**
     * 全量查询部门列表（用于前端下拉选择）
     *
     * @param orgId 机构ID（非必填，传入则只查询该机构下的部门）
     * @return 部门列表（包含关联名称）
     */
    List<DeptVO> listAllDept(Long orgId);

    /**
     * 根据部门ID查询关联机构列表
     *
     * @param id 部门ID
     * @return 关联机构列表
     */
    List<DeptVO.OrgSimpleVO> listOrgsByDeptId(Long id);
}
