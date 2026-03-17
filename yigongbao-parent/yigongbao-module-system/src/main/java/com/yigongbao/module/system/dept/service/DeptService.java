package com.yigongbao.module.system.dept.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.system.dept.dto.CreateDeptDTO;
import com.yigongbao.module.system.dept.dto.UpdateDeptDTO;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import com.yigongbao.module.system.dept.vo.DeptVO;

/**
 * 部门 Service 接口
 *
 * @author hanjor
 * @date 2026-03-17
 */
public interface DeptService extends IService<DeptEntity> {

    /**
     * 分页查询部门列表
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param orgId    所属机构ID
     * @param deptName 部门名称（模糊查询）
     * @param status   状态
     * @return 分页后的部门列表
     */
    IPage<DeptVO> listDept(Integer pageNum, Integer pageSize, Long orgId, String deptName, Integer status);

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
}
