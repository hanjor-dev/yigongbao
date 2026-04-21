package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.FileBizTypeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.entity.DesignDrawingEntity;
import com.yigongbao.module.design.entity.DesignInstructionEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.entity.DesignProductFileEntity;
import com.yigongbao.module.design.helper.DrawingExcelBuilder;
import com.yigongbao.module.design.helper.InstructionExcelBuilder;
import com.yigongbao.module.design.service.DesignDocService;
import com.yigongbao.module.design.service.DesignDrawingService;
import com.yigongbao.module.design.service.DesignInstructionService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.service.DesignProductFileService;
import com.yigongbao.module.design.service.DesignProductService;
import com.yigongbao.module.design.service.DesignScreenshotService;
import com.yigongbao.module.design.vo.DesignDocVersionVO;
import com.yigongbao.module.design.vo.DocItemVO;
import com.yigongbao.module.order.service.OrderMainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 指令单/图纸生成与管理服务实现类
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DesignDocServiceImpl implements DesignDocService {

    private final OrderMainService orderMainService;
    private final DesignPackageService packageService;
    private final DesignProductService productService;
    private final DesignProductFileService productFileService;
    private final DesignInstructionService instructionService;
    private final DesignDrawingService drawingService;
    private final InstructionExcelBuilder instructionBuilder;
    private final DrawingExcelBuilder drawingBuilder;
    private final CodeGeneratorService codeGeneratorService;
    private final FileService fileService;
    private final DesignScreenshotService screenshotService;

    /**
     * 生成指令单
     * <p>
     * 版本策略：若最新版尚未上传修订版（revisedFileId==null），覆盖其模板文件，版本号不变；
     * 若最新版已封版（revisedFileId 非空）或首次生成，新建记录，版本序号+1。
     * </p>
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 生成结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocItemVO generateInstruction(Long orderId, Long packageId) {
        log.info("开始生成指令单，orderId={}, packageId={}", orderId, packageId);

        // 1. 权限校验
        OrderMainEntity order = checkOrderAndPermission(orderId);
        DesignPackageEntity pkg = validatePackage(orderId, packageId);

        // 2. 前置校验：打印信息已填写
        long productCount = productService.count(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId));
        if (productCount == 0) {
            throw new BusinessException(ErrorCodeEnum.PRINT_INFO_REQUIRED);
        }

        // 3. 查询最新版本，判断是否需要新建版本
        DesignInstructionEntity latest = instructionService.getLatestVersion(packageId);
        boolean isNewVersion = (latest == null || latest.getRevisedFileId() != null);
        int newSeq = isNewVersion ? (latest == null ? 1 : latest.getVersionSeq() + 1)
                : latest.getVersionSeq();
        String version = "A/" + newSeq;
        log.info("指令单版本：{}，isNewVersion={}，packageId={}", version, isNewVersion, packageId);

        // 4. 查询打印产品列表并展开为产品×文件行
        List<DesignProductEntity> products = productService.list(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId)
                        .orderByAsc(DesignProductEntity::getSortOrder));
        List<InstructionExcelBuilder.ProductRow> rows = expandProductRows(products);

        // 5. 生成指令单 Excel
        LocalDateTime now = LocalDateTime.now();
        String instructionCode = isNewVersion
                ? codeGeneratorService.generate(CodeRuleConstants.INSTRUCTION_NO)
                : latest.getInstructionCode();
        InstructionExcelBuilder.BuildContext instrCtx = buildInstructionContext(order, pkg, rows, version, now);
        byte[] instrBytes;
        try {
            instrBytes = instructionBuilder.build(instrCtx);
        } catch (IOException e) {
            log.error("生成指令单 Excel 失败，packageId={}", packageId, e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }

        // 6. 上传文件
        String filename = "指令单-" + pkg.getPackageCode() + ".xlsx";
        FileVO instrFile = fileService.uploadBytes(instrBytes, filename, FileBizTypeEnum.INSTRUCTION_FILE.getDictCode());

        // 7. 新建记录或更新模板文件
        DesignInstructionEntity instrEntity;
        if (isNewVersion) {
            instrEntity = new DesignInstructionEntity();
            instrEntity.setOrderId(orderId);
            instrEntity.setPackageId(packageId);
            instrEntity.setInstructionCode(instructionCode);
            instrEntity.setVersion(version);
            instrEntity.setVersionSeq(newSeq);
            instrEntity.setGenerateTime(now);
            instrEntity.setTemplateFileId(instrFile.getId());
            instrEntity.setTemplateFileUrl(instrFile.getFileUrl());
            // 新版本初始为未确认状态，需设计师确认（在线模式）或上传修订版自动确认（离线模式）
            instrEntity.setIsConfirmed(0);
            instructionService.save(instrEntity);
        } else {
            latest.setTemplateFileId(instrFile.getId());
            latest.setTemplateFileUrl(instrFile.getFileUrl());
            latest.setGenerateTime(now);
            // 重新生成指令单时重置确认状态，强制重新确认
            latest.setIsConfirmed(0);
            latest.setConfirmTime(null);
            instructionService.updateById(latest);
            instrEntity = latest;
        }

        // 8. 构造返回值
        DocItemVO vo = new DocItemVO();
        vo.setId(instrEntity.getId());
        vo.setVersion(version);
        vo.setFileId(instrFile.getId());
        vo.setTemplateFileUrl(instrFile.getFileUrl());
        vo.setGenerateTime(instrEntity.getGenerateTime());

        log.info("生成指令单完成，orderId={}, packageId={}, version={}", orderId, packageId, version);
        return vo;
    }

    /**
     * 生成图纸
     * <p>
     * 版本策略：若最新版尚未上传修订版（revisedFileId==null），覆盖其模板文件，版本号不变；
     * 若最新版已封版（revisedFileId 非空）或首次生成，新建记录，版本序号+1。
     * </p>
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 生成结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocItemVO generateDrawing(Long orderId, Long packageId) {
        log.info("开始生成图纸，orderId={}, packageId={}", orderId, packageId);

        // 1. 权限校验
        OrderMainEntity order = checkOrderAndPermission(orderId);
        DesignPackageEntity pkg = validatePackage(orderId, packageId);

        // 2. 前置校验：打印信息已填写
        long productCount = productService.count(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId));
        if (productCount == 0) {
            throw new BusinessException(ErrorCodeEnum.PRINT_INFO_REQUIRED);
        }

        // 3. 查询最新版本，判断是否需要新建版本
        DesignDrawingEntity latest = drawingService.getLatestVersion(packageId);
        boolean isNewVersion = (latest == null || latest.getRevisedFileId() != null);
        int newSeq = isNewVersion ? (latest == null ? 1 : latest.getVersionSeq() + 1)
                : latest.getVersionSeq();
        String version = "A/" + newSeq;
        log.info("图纸版本：{}，isNewVersion={}，packageId={}", version, isNewVersion, packageId);

        // 4. 查询打印产品列表并展开为产品×文件行
        List<DesignProductEntity> products = productService.list(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId)
                        .orderByAsc(DesignProductEntity::getSortOrder));
        List<Long> productIds = products.stream().map(DesignProductEntity::getId).toList();
        List<DesignProductFileEntity> allProductFiles = productFileService.listByProductIds(productIds);

        // 5. 批量查询截图（packageFileId → bytes），下载截图字节
        List<Long> packageFileIds = allProductFiles.stream()
                .map(DesignProductFileEntity::getPackageFileId).distinct().toList();
        Map<Long, String> screenshotFileIdMap = screenshotService.listFileIdsByPackageFileIds(packageFileIds);
        Map<Long, byte[]> screenshotBytesMap = loadScreenshotBytes(screenshotFileIdMap);

        List<DrawingExcelBuilder.ProductRow> drawingRows = expandDrawingRows(products, allProductFiles, screenshotBytesMap);

        // 6. 生成图纸 Excel
        LocalDateTime now = LocalDateTime.now();
        DrawingExcelBuilder.BuildContext drawCtx = buildDrawingContext(order, pkg, drawingRows, now);
        byte[] drawBytes;
        try {
            drawBytes = drawingBuilder.build(drawCtx);
        } catch (IOException e) {
            log.error("生成图纸 Excel 失败，packageId={}", packageId, e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }

        // 7. 上传文件
        String filename = "图纸-" + pkg.getPackageCode() + ".xlsx";
        FileVO drawFile = fileService.uploadBytes(drawBytes, filename, FileBizTypeEnum.DRAWING_FILE.getDictCode());

        // 8. 新建记录或更新模板文件
        DesignDrawingEntity drawEntity;
        if (isNewVersion) {
            drawEntity = new DesignDrawingEntity();
            drawEntity.setOrderId(orderId);
            drawEntity.setPackageId(packageId);
            drawEntity.setVersion(version);
            drawEntity.setVersionSeq(newSeq);
            drawEntity.setGenerateTime(now);
            drawEntity.setTemplateFileId(drawFile.getId());
            drawEntity.setTemplateFileUrl(drawFile.getFileUrl());
            // 新版本初始为未确认状态，需设计师预览后手动确认（在线模式）或上传修订版自动确认（离线模式）
            drawEntity.setIsConfirmed(0);
            drawingService.save(drawEntity);
        } else {
            latest.setTemplateFileId(drawFile.getId());
            latest.setTemplateFileUrl(drawFile.getFileUrl());
            latest.setGenerateTime(now);
            // 重新生成图纸时重置确认状态，强制重新确认
            latest.setIsConfirmed(0);
            latest.setConfirmTime(null);
            drawingService.updateById(latest);
            drawEntity = latest;
        }

        // 9. 构造返回值
        DocItemVO vo = new DocItemVO();
        vo.setId(drawEntity.getId());
        vo.setVersion(version);
        vo.setFileId(drawFile.getId());
        vo.setTemplateFileUrl(drawFile.getFileUrl());
        vo.setGenerateTime(drawEntity.getGenerateTime());

        log.info("生成图纸完成，orderId={}, packageId={}, version={}", orderId, packageId, version);
        return vo;
    }

    /**
     * 查询指令单版本历史列表
     */
    @Override
    public List<DesignDocVersionVO> listInstructionVersions(Long orderId, Long packageId) {
        log.info("查询指令单版本列表，orderId={}, packageId={}", orderId, packageId);
        validatePackage(orderId, packageId);
        return instructionService.listVersions(packageId).stream()
                .map(this::toInstructionVersionVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询图纸版本历史列表
     */
    @Override
    public List<DesignDocVersionVO> listDrawingVersions(Long orderId, Long packageId) {
        log.info("查询图纸版本列表，orderId={}, packageId={}", orderId, packageId);
        validatePackage(orderId, packageId);
        return drawingService.listVersions(packageId).stream()
                .map(this::toDrawingVersionVO)
                .collect(Collectors.toList());
    }

    /**
     * 上传修订版指令单
     * <p>
     * 离线模式：上传即视为已确认（is_confirmed=1），无需额外操作。
     * 在线模式：上传不改变确认状态，设计师仍需手动调用 confirmInstruction 确认。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadRevisedInstruction(Long orderId, Long packageId, Long id, MultipartFile file) {
        log.info("上传修订版指令单，orderId={}, packageId={}, id={}", orderId, packageId, id);
        checkOrderAndPermission(orderId);
        validatePackage(orderId, packageId);
        DesignInstructionEntity entity = instructionService.getById(id);
        if (entity == null || !entity.getPackageId().equals(packageId)) {
            throw new BusinessException(ErrorCodeEnum.DOC_VERSION_NOT_FOUND);
        }
        FileVO fileVO = fileService.uploadFile(file, FileBizTypeEnum.INSTRUCTION_FILE.getDictCode());
        LocalDateTime uploadTime = LocalDateTime.now();
        entity.setRevisedFileId(fileVO.getId());
        entity.setRevisedFileUrl(fileVO.getFileUrl());
        entity.setRevisedUploadTime(uploadTime);
        // 上传修订版本身即代表设计师已审阅，无论在线/离线模式均自动确认
        entity.setIsConfirmed(1);
        entity.setConfirmTime(uploadTime);
        instructionService.updateById(entity);
        log.info("上传修订版指令单成功，已自动确认，id={}", id);
    }

    /**
     * 上传修订版图纸
     * <p>
     * 离线模式：上传即视为已确认（is_confirmed=1），无需额外操作。
     * 在线模式：上传不改变确认状态，设计师仍需手动调用 confirmDrawing 确认。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadRevisedDrawing(Long orderId, Long packageId, Long id, MultipartFile file) {
        log.info("上传修订版图纸，orderId={}, packageId={}, id={}", orderId, packageId, id);
        checkOrderAndPermission(orderId);
        validatePackage(orderId, packageId);
        DesignDrawingEntity entity = drawingService.getById(id);
        if (entity == null || !entity.getPackageId().equals(packageId)) {
            throw new BusinessException(ErrorCodeEnum.DOC_VERSION_NOT_FOUND);
        }
        FileVO fileVO = fileService.uploadFile(file, FileBizTypeEnum.DRAWING_FILE.getDictCode());
        LocalDateTime uploadTime = LocalDateTime.now();
        entity.setRevisedFileId(fileVO.getId());
        entity.setRevisedFileUrl(fileVO.getFileUrl());
        entity.setRevisedUploadTime(uploadTime);
        // 上传修订版本身即代表设计师已审阅，无论在线/离线模式均自动确认
        entity.setIsConfirmed(1);
        entity.setConfirmTime(uploadTime);
        drawingService.updateById(entity);
        log.info("上传修订版图纸成功，已自动确认，id={}", id);
    }

    /**
     * 确认图纸（在线模式专用）
     * <p>
     * 设计师预览生成的图纸满意后调用，将 is_confirmed 置为 1。
     * 若之后重新生成图纸，is_confirmed 会被自动重置为 0。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDrawing(Long orderId, Long packageId, Long id) {
        log.info("确认图纸，orderId={}, packageId={}, id={}", orderId, packageId, id);
        checkOrderAndPermission(orderId);
        validatePackage(orderId, packageId);
        DesignDrawingEntity entity = drawingService.getById(id);
        if (entity == null || !entity.getPackageId().equals(packageId)) {
            throw new BusinessException(ErrorCodeEnum.DOC_VERSION_NOT_FOUND);
        }
        entity.setIsConfirmed(1);
        entity.setConfirmTime(LocalDateTime.now());
        drawingService.updateById(entity);
        log.info("确认图纸成功，id={}", id);
    }

    /**
     * 确认指令单（在线模式专用）
     * <p>
     * 设计师确认生成的指令单内容无误后调用，将 is_confirmed 置为 1。
     * 若之后重新生成指令单，is_confirmed 会被自动重置为 0。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmInstruction(Long orderId, Long packageId, Long id) {
        log.info("确认指令单，orderId={}, packageId={}, id={}", orderId, packageId, id);
        checkOrderAndPermission(orderId);
        validatePackage(orderId, packageId);
        DesignInstructionEntity entity = instructionService.getById(id);
        if (entity == null || !entity.getPackageId().equals(packageId)) {
            throw new BusinessException(ErrorCodeEnum.DOC_VERSION_NOT_FOUND);
        }
        entity.setIsConfirmed(1);
        entity.setConfirmTime(LocalDateTime.now());
        instructionService.updateById(entity);
        log.info("确认指令单成功，id={}", id);
    }

    // ==================== 私有方法 ====================

    /**
     * 将产品行 + 文件关联展开为指令单行列表（一个文件=一行）
     */
    private List<InstructionExcelBuilder.ProductRow> expandProductRows(
            List<DesignProductEntity> products) {
        if (products.isEmpty()) return List.of();
        List<Long> productIds = products.stream().map(DesignProductEntity::getId).toList();
        List<DesignProductFileEntity> allFiles = productFileService.listByProductIds(productIds);
        Map<Long, List<DesignProductFileEntity>> fileMap = allFiles.stream()
                .collect(Collectors.groupingBy(DesignProductFileEntity::getDesignProductId));

        List<InstructionExcelBuilder.ProductRow> rows = new ArrayList<>();
        for (DesignProductEntity p : products) {
            List<DesignProductFileEntity> files = fileMap.getOrDefault(p.getId(), List.of());
            for (DesignProductFileEntity f : files) {
                InstructionExcelBuilder.ProductRow row = new InstructionExcelBuilder.ProductRow();
                row.setCertNo(p.getCertNo());
                row.setProductName(p.getProductName());
                row.setPackageFileName(f.getPackageFileName());
                row.setSpecName(p.getSpecName());
                row.setMaterialName(p.getMaterialName());
                row.setQuantity(p.getQuantity());
                row.setIsUrgent(p.getIsUrgent()); // 行级 is_urgent
                row.setColorName(p.getColorName());
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * 将产品行 + 文件关联展开为图纸行列表（一个文件=一行，附截图字节）
     */
    private List<DrawingExcelBuilder.ProductRow> expandDrawingRows(
            List<DesignProductEntity> products,
            List<DesignProductFileEntity> allFiles,
            Map<Long, byte[]> screenshotBytesMap) {
        if (products.isEmpty()) return List.of();
        Map<Long, List<DesignProductFileEntity>> fileMap = allFiles.stream()
                .collect(Collectors.groupingBy(DesignProductFileEntity::getDesignProductId));

        List<DrawingExcelBuilder.ProductRow> rows = new ArrayList<>();
        for (DesignProductEntity p : products) {
            List<DesignProductFileEntity> files = fileMap.getOrDefault(p.getId(), List.of());
            for (DesignProductFileEntity f : files) {
                DrawingExcelBuilder.ProductRow row = new DrawingExcelBuilder.ProductRow();
                row.setPackageFileName(f.getPackageFileName());
                row.setProductName(p.getProductName());
                // 填充截图字节（无截图时为 null，builder 忽略）
                row.setScreenshotBytes(screenshotBytesMap.get(f.getPackageFileId()));
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * 批量下载截图文件字节（packageFileId → bytes）
     * 下载失败时记录 warn 日志并跳过，不影响图纸生成
     */
    private Map<Long, byte[]> loadScreenshotBytes(Map<Long, String> screenshotFileIdMap) {
        if (screenshotFileIdMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, byte[]> result = new java.util.HashMap<>();
        for (Map.Entry<Long, String> entry : screenshotFileIdMap.entrySet()) {
            try {
                byte[] bytes = fileService.downloadToBytes(entry.getValue());
                result.put(entry.getKey(), bytes);
            } catch (Exception e) {
                log.warn("截图文件下载失败，packageFileId={}，fileId={}，跳过嵌图",
                        entry.getKey(), entry.getValue(), e);
            }
        }
        return result;
    }

    /**
     * 校验订单状态和操作权限（设计中或设计审核不通过时，当前用户是订单设计师）
     */
    private OrderMainEntity checkOrderAndPermission(Long orderId) {
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        FlowStatusEnum status = FlowStatusEnum.getByValue(order.getStatus());
        if (status == null || !status.belongsTo(FlowPhaseEnum.DESIGN)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED);
        }
        if (status != FlowStatusEnum.DESIGN_IN_PROGRESS
                && status != FlowStatusEnum.DESIGN_REVIEW_REJECTED) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED);
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!currentUserId.equals(order.getDesignerId())) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_OPERATOR_NOT_ALLOWED);
        }
        return order;
    }

    /**
     * 校验 packageId 属于 orderId
     */
    private DesignPackageEntity validatePackage(Long orderId, Long packageId) {
        DesignPackageEntity pkg = packageService.getById(packageId);
        if (pkg == null || !pkg.getOrderId().equals(orderId)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND);
        }
        return pkg;
    }

    /**
     * 构建指令单填充上下文
     */
    private InstructionExcelBuilder.BuildContext buildInstructionContext(
            OrderMainEntity order, DesignPackageEntity pkg,
            List<InstructionExcelBuilder.ProductRow> rows, String version, LocalDateTime generateTime) {
        InstructionExcelBuilder.BuildContext ctx = new InstructionExcelBuilder.BuildContext();
        ctx.setOrderCode(order.getOrderCode());
        ctx.setPatientName(order.getPatientName());
        ctx.setHospitalName(order.getHospitalName());
        ctx.setContactName(order.getDoctorName());
        ctx.setPackageCode(pkg.getPackageCode());
        ctx.setVersion(version);
        ctx.setRows(rows);
        ctx.setProductMark(pkg.getProductMark());
        ctx.setPackQuantity(pkg.getPackQuantity());
        ctx.setRemark(pkg.getRemark());
        ctx.setDesignerName(order.getDesignerName());
        if (order.getExpectedDeliveryDate() != null) {
            ctx.setExpectedDeliveryDate(order.getExpectedDeliveryDate()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        if (order.getDesignStartTime() != null) {
            ctx.setDesignStartTime(order.getDesignStartTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
        ctx.setGenerateDate(generateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        ctx.setGenerateTime(generateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        ctx.setPostalAddress(order.getPostalAddress());
        ctx.setIsPostal(order.getIsPostal() != null && order.getIsPostal() == 1 ? "是" : "否");
        return ctx;
    }

    /**
     * 构建图纸填充上下文
     */
    private DrawingExcelBuilder.BuildContext buildDrawingContext(
            OrderMainEntity order, DesignPackageEntity pkg,
            List<DrawingExcelBuilder.ProductRow> rows, LocalDateTime generateTime) {
        DrawingExcelBuilder.BuildContext ctx = new DrawingExcelBuilder.BuildContext();
        ctx.setOrderCode(order.getOrderCode());
        ctx.setPackageCode(pkg.getPackageCode());
        ctx.setRemark(pkg.getRemark());
        ctx.setRows(rows);
        ctx.setDesignerName(order.getDesignerName());
        ctx.setGenerateDate(generateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        return ctx;
    }

    private DesignDocVersionVO toInstructionVersionVO(DesignInstructionEntity entity) {
        DesignDocVersionVO vo = new DesignDocVersionVO();
        vo.setId(entity.getId());
        vo.setVersion(entity.getVersion());
        vo.setVersionSeq(entity.getVersionSeq());
        vo.setTemplateFileId(entity.getTemplateFileId());
        vo.setTemplateFileUrl(entity.getTemplateFileUrl());
        vo.setRevisedFileId(entity.getRevisedFileId());
        vo.setRevisedFileUrl(entity.getRevisedFileUrl());
        vo.setGenerateTime(entity.getGenerateTime());
        vo.setRevisedUploadTime(entity.getRevisedUploadTime());
        vo.setIsConfirmed(entity.getIsConfirmed());
        vo.setConfirmTime(entity.getConfirmTime());
        return vo;
    }

    private DesignDocVersionVO toDrawingVersionVO(DesignDrawingEntity entity) {
        DesignDocVersionVO vo = new DesignDocVersionVO();
        vo.setId(entity.getId());
        vo.setVersion(entity.getVersion());
        vo.setVersionSeq(entity.getVersionSeq());
        vo.setTemplateFileId(entity.getTemplateFileId());
        vo.setTemplateFileUrl(entity.getTemplateFileUrl());
        vo.setRevisedFileId(entity.getRevisedFileId());
        vo.setRevisedFileUrl(entity.getRevisedFileUrl());
        vo.setGenerateTime(entity.getGenerateTime());
        vo.setRevisedUploadTime(entity.getRevisedUploadTime());
        vo.setIsConfirmed(entity.getIsConfirmed());
        vo.setConfirmTime(entity.getConfirmTime());
        return vo;
    }

    /**
     * 批量查询数据包最新版指令单，返回 packageId → DesignDocVersionVO 映射
     * 无记录的包不出现在结果 map 中，用于工单详情一次性填充所有数据包的指令单状态
     *
     * @param packageIds 数据包ID集合
     * @return key=packageId，value=最新版指令单 VO
     */
    @Override
    public Map<Long, DesignDocVersionVO> getLatestInstructionMap(Collection<Long> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 查询所有相关指令单，按 versionSeq 倒序，取各包最新一条
        List<DesignInstructionEntity> all = instructionService.list(
                new LambdaQueryWrapper<DesignInstructionEntity>()
                        .in(DesignInstructionEntity::getPackageId, packageIds)
                        .orderByDesc(DesignInstructionEntity::getVersionSeq));
        Map<Long, DesignDocVersionVO> result = new java.util.LinkedHashMap<>();
        for (DesignInstructionEntity entity : all) {
            result.putIfAbsent(entity.getPackageId(), toInstructionVersionVO(entity));
        }
        return result;
    }

    /**
     * 批量查询数据包最新版图纸，返回 packageId → DesignDocVersionVO 映射
     * 无记录的包不出现在结果 map 中，用于工单详情一次性填充所有数据包的图纸状态
     *
     * @param packageIds 数据包ID集合
     * @return key=packageId，value=最新版图纸 VO
     */
    @Override
    public Map<Long, DesignDocVersionVO> getLatestDrawingMap(Collection<Long> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DesignDrawingEntity> all = drawingService.list(
                new LambdaQueryWrapper<DesignDrawingEntity>()
                        .in(DesignDrawingEntity::getPackageId, packageIds)
                        .orderByDesc(DesignDrawingEntity::getVersionSeq));
        Map<Long, DesignDocVersionVO> result = new java.util.LinkedHashMap<>();
        for (DesignDrawingEntity entity : all) {
            result.putIfAbsent(entity.getPackageId(), toDrawingVersionVO(entity));
        }
        return result;
    }
}
