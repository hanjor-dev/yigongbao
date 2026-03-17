package com.yigongbao.module.system.org.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.system.org.dto.CreateOrgDTO;
import com.yigongbao.module.system.org.dto.UpdateOrgDTO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.vo.OrgVO;

/**
 * 机构 Service 接口
 *
 * @author hanjor
 * @date 2026-03-16
 */
public interface OrgService extends IService<OrgEntity> {

    /**
     * 分页查询机构列表
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param orgName  机构名称（模糊查询）
     * @param orgType  机构类型
     * @param areaId   地区ID
     * @param status   状态
     * @return 分页后的机构列表
     */
    IPage<OrgVO> listOrg(Integer pageNum, Integer pageSize, String orgName, Integer orgType, Long areaId, Integer status);

    /**
     * 根据ID查询机构详情
     *
     * @param id 机构ID
     * @return 机构详情
     */
    OrgVO getOrgById(Long id);

    /**
     * 创建机构
     *
     * @param dto 创建参数
     */
    void createOrg(CreateOrgDTO dto);

    /**
     * 更新机构
     *
     * @param id  机构ID
     * @param dto 更新参数
     */
    void updateOrg(Long id, UpdateOrgDTO dto);

    /**
     * 删除机构
     *
     * @param id 机构ID
     */
    void removeOrg(Long id);

    /**
     * 修改机构状态
     *
     * @param id     机构ID
     * @param status 状态（0=禁用，1=正常）
     */
    void updateStatus(Long id, Integer status);
}
