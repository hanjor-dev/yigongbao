package com.yigongbao.module.design.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.dto.SaveDesignColumnConfigDTO;
import com.yigongbao.module.design.vo.DesignColumnConfigVO;
import com.yigongbao.module.design.vo.DesignWorkorderDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;

/**
 * 设计工单查询服务接口
 *
 * @author hanjor
 * @date 2026-04-16
 */
public interface DesignWorkorderService {

    /**
     * 分页查询设计工单列表
     * 根据当前用户的数据权限范围，仅返回设计阶段（phase=20）的工单
     *
     * @param queryDTO 查询参数（分页、筛选、排序）
     * @return 分页工单列表
     */
    IPage<DesignWorkorderListVO> listWorkorders(DesignWorkorderQueryDTO queryDTO);

    /**
     * 获取工单详情
     * 包含订单基本信息、重建项目、提交校验状态
     *
     * @param orderId 订单ID
     * @return 工单详情 VO
     */
    DesignWorkorderDetailVO getWorkorderDetail(Long orderId);

    /**
     * 获取当前用户的列配置
     * 优先返回用户个人配置，无则返回系统默认配置
     *
     * @return 列配置 VO
     */
    DesignColumnConfigVO getColumnConfig();

    /**
     * 保存当前用户的列配置到 sys_user.design_column_settings
     *
     * @param dto 列配置参数
     */
    void saveColumnConfig(SaveDesignColumnConfigDTO dto);
}
