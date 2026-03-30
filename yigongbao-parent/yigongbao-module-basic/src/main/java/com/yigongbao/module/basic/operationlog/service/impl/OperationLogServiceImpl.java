package com.yigongbao.module.basic.operationlog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.service.OperationLogService;
import com.yigongbao.module.basic.operationlog.convert.OperationLogConvert;
import com.yigongbao.module.basic.operationlog.dto.OperationLogQueryDTO;
import com.yigongbao.module.basic.operationlog.entity.OperationLogEntity;
import com.yigongbao.module.basic.operationlog.mapper.OperationLogMapper;
import com.yigongbao.module.basic.operationlog.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 操作日志 Service 实现类
 * <p>
 * 【架构设计说明】
 * 本类实现了两个 Service 接口：
 * <ul>
 *   <li>basic.operationlog.service.OperationLogService：提供业务查询（pageLogs、exportLogs）
 *       和 CRUD 操作，面向 Controller 层调用</li>
 *   <li>common.service.OperationLogService：提供 AOP 切面回调方法（saveLog），
 *       面向 OperationLogAspect 框架层调用</li>
 * </ul>
 * <p>
 * 这种设计的 trade-off：
 * - 优点：common 层通过接口依赖解耦了 basic 模块，framework 层不需要直接依赖 basic 模块；
 *         避免了循环依赖（basic → common → framework，而 framework 的 AOP 需要调用日志保存）
 * - 缺点：违反"一个类实现一个接口"的单一职责原则，业务层查询逻辑和框架层回调逻辑混杂
 * <p>
 * 替代方案（未来重构参考）：
 * - 方案1：将 saveLog 拆分为 BasicOperationLogServiceImpl（basic 模块），提供 AOP 回调；
 *          新建 CommonOperationLogServiceImpl（common 模块），通过 Spring 代理委托给 basic 实现
 * - 方案2：将 OperationLogAspect 移入 basic 模块，消除 framework 层对日志保存的依赖
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLogEntity>
        implements com.yigongbao.module.basic.operationlog.service.OperationLogService,
                   com.yigongbao.common.service.OperationLogService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 异步保存操作日志（AOP 切面调用）
     *
     * @param operationType 操作类型
     * @param module 模块名称
     * @param businessNo 业务编号
     * @param content 操作内容
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @param requestMethod 请求方法（GET/POST/PUT/DELETE）
     * @param duration 执行时长（毫秒）
     * @param success 是否成功
     * @param errorMessage 错误信息
     */
    @Override
    public void saveLog(OperationTypeEnum operationType, String module, String businessNo,
                         String content, Long operatorId, String operatorName, String operatorUsername,
                         String ipAddress, String userAgent, String requestMethod,
                         Long duration, boolean success, String errorMessage, String requestParams) {
        OperationLogEntity logEntity = new OperationLogEntity();
        logEntity.setModule(module);
        logEntity.setBusinessType(operationType != null ? operationType.getCode() : null);
        logEntity.setBusinessTypeName(operationType != null ? operationType.getDescription() : null);
        logEntity.setOperation(operationType != null ? operationType.getDescription() : null);
        logEntity.setDescription(content);
        logEntity.setRequestUrl(businessNo);
        logEntity.setRequestParams(requestParams);
        logEntity.setIp(ipAddress);
        logEntity.setUserId(operatorId);
        logEntity.setRealName(operatorName);
        logEntity.setUsername(operatorUsername);
        logEntity.setUserAgent(userAgent);
        logEntity.setRequestMethod(requestMethod);
        logEntity.setOperationTime(LocalDateTime.now());
        logEntity.setDuration(duration);
        logEntity.setStatus(success ? StatusConstants.NORMAL : StatusConstants.DISABLED);
        logEntity.setErrorMessage(errorMessage);
        save(logEntity);
    }

    /**
     * 分页查询日志
     */
    @Override
    public IPage<OperationLogVO> pageLogs(OperationLogQueryDTO dto) {
        log.info("分页查询操作日志，dto={}", dto);
        try {
            Page<OperationLogEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
            LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<>();

            wrapper.like(dto.getModule() != null && !dto.getModule().isEmpty(),
                    OperationLogEntity::getModule, dto.getModule())
                    .eq(dto.getBusinessType() != null,
                            OperationLogEntity::getBusinessType, dto.getBusinessType())
                    .like(dto.getOperation() != null && !dto.getOperation().isEmpty(),
                            OperationLogEntity::getOperation, dto.getOperation())
                    .like(dto.getUsername() != null && !dto.getUsername().isEmpty(),
                            OperationLogEntity::getUsername, dto.getUsername())
                    .like(dto.getIp() != null && !dto.getIp().isEmpty(),
                            OperationLogEntity::getIp, dto.getIp())
                    .eq(dto.getStatus() != null,
                            OperationLogEntity::getStatus, dto.getStatus());
            if (dto.getStartTime() != null && !dto.getStartTime().isEmpty()) {
                LocalDateTime startDateTime = LocalDateTime.parse(dto.getStartTime() + " 00:00:00", DATE_TIME_FORMATTER);
                wrapper.ge(OperationLogEntity::getOperationTime, startDateTime);
            }
            if (dto.getEndTime() != null && !dto.getEndTime().isEmpty()) {
                LocalDateTime endDateTime = LocalDateTime.parse(dto.getEndTime() + " 23:59:59", DATE_TIME_FORMATTER);
                wrapper.le(OperationLogEntity::getOperationTime, endDateTime);
            }
            wrapper.orderByDesc(OperationLogEntity::getOperationTime);

            IPage<OperationLogEntity> pageResult = page(page, wrapper);

            // 转换为 VO
            IPage<OperationLogVO> voPage = pageResult.convert(entity -> {
                OperationLogVO vo = OperationLogConvert.toVO(entity);
                if (vo.getStatus() != null) {
                    vo.setStatusName(StatusConstants.getOperationResultName(vo.getStatus()));
                }
                return vo;
            });

            log.info("分页查询操作日志成功，总数={}", pageResult.getTotal());
            return voPage;
        } catch (Exception e) {
            log.error("分页查询操作日志异常", e);
            throw e;
        }
    }

    /**
     * 导出日志
     */
    @Override
    public void exportLogs(OperationLogQueryDTO dto, jakarta.servlet.http.HttpServletResponse response) {
        log.info("导出操作日志，dto={}", dto);
        try {
            LambdaQueryWrapper<OperationLogEntity> wrapper = new LambdaQueryWrapper<>();

            wrapper.like(dto.getModule() != null && !dto.getModule().isEmpty(),
                    OperationLogEntity::getModule, dto.getModule())
                    .eq(dto.getBusinessType() != null,
                            OperationLogEntity::getBusinessType, dto.getBusinessType())
                    .like(dto.getOperation() != null && !dto.getOperation().isEmpty(),
                            OperationLogEntity::getOperation, dto.getOperation())
                    .like(dto.getUsername() != null && !dto.getUsername().isEmpty(),
                            OperationLogEntity::getUsername, dto.getUsername())
                    .like(dto.getIp() != null && !dto.getIp().isEmpty(),
                            OperationLogEntity::getIp, dto.getIp())
                    .eq(dto.getStatus() != null,
                            OperationLogEntity::getStatus, dto.getStatus());
            if (dto.getStartTime() != null && !dto.getStartTime().isEmpty()) {
                LocalDateTime startDateTime = LocalDateTime.parse(dto.getStartTime() + " 00:00:00", DATE_TIME_FORMATTER);
                wrapper.ge(OperationLogEntity::getOperationTime, startDateTime);
            }
            if (dto.getEndTime() != null && !dto.getEndTime().isEmpty()) {
                LocalDateTime endDateTime = LocalDateTime.parse(dto.getEndTime() + " 23:59:59", DATE_TIME_FORMATTER);
                wrapper.le(OperationLogEntity::getOperationTime, endDateTime);
            }
            wrapper.orderByDesc(OperationLogEntity::getOperationTime)
                    .last("LIMIT 10000");

            List<OperationLogEntity> list = list(wrapper);

            // 创建 Excel 工作簿
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("操作日志");

            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"序号", "模块", "操作类型", "操作描述", "请求方法", "请求URL",
                    "请求参数", "IP地址", "用户", "状态", "耗时(ms)", "操作时间", "错误信息"};
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // 填充数据
            int rowNum = 1;
            for (OperationLogEntity entity : list) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 1);
                row.createCell(1).setCellValue(entity.getModule());
                row.createCell(2).setCellValue(entity.getBusinessTypeName());
                row.createCell(3).setCellValue(entity.getOperation());
                row.createCell(4).setCellValue(entity.getRequestMethod());
                row.createCell(5).setCellValue(entity.getRequestUrl());
                row.createCell(6).setCellValue(entity.getRequestParams());
                row.createCell(7).setCellValue(entity.getIp());
                row.createCell(8).setCellValue(
                        cn.hutool.core.util.StrUtil.isNotBlank(entity.getRealName())
                                ? entity.getRealName() : entity.getUsername());
                row.createCell(9).setCellValue(entity.getStatus() != null
                        ? StatusConstants.getOperationResultName(entity.getStatus()) : "");
                row.createCell(10).setCellValue(entity.getDuration() != null ? entity.getDuration() : 0);
                row.createCell(11).setCellValue(entity.getOperationTime() != null
                        ? entity.getOperationTime().format(DATE_TIME_FORMATTER) : "");
                row.createCell(12).setCellValue(entity.getErrorMessage());
            }

            // 设置响应头
            String fileName = "操作日志_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8));

            workbook.write(response.getOutputStream());
            workbook.close();

            log.info("导出操作日志成功，共{}条", list.size());
        } catch (IOException e) {
            log.error("导出操作日志异常", e);
            throw new BusinessException(ErrorCodeEnum.LOG_EXPORT_FAILED);
        }
    }
}
