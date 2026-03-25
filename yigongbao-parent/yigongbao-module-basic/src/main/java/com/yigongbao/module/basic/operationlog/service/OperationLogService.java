package com.yigongbao.module.basic.operationlog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.operationlog.dto.OperationLogQueryDTO;
import com.yigongbao.module.basic.operationlog.entity.OperationLogEntity;
import com.yigongbao.module.basic.operationlog.vo.OperationLogVO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 操作日志 Service 接口
 *
 * @author hanjor
 * @date 2026-03-24
 */
public interface OperationLogService extends IService<OperationLogEntity> {

    /**
     * 分页查询日志
     *
     * @param dto 查询参数
     * @return 分页结果
     */
    IPage<OperationLogVO> pageLogs(OperationLogQueryDTO dto);

    /**
     * 导出日志
     *
     * @param dto 查询参数
     * @param response HTTP响应
     */
    void exportLogs(OperationLogQueryDTO dto, HttpServletResponse response);
}
