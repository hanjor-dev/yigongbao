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
import com.yigongbao.module.design.entity.DesignDrawingEntity;
import com.yigongbao.module.design.entity.DesignInstructionEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.helper.DrawingExcelBuilder;
import com.yigongbao.module.design.helper.InstructionExcelBuilder;
import com.yigongbao.module.design.service.DesignDocService;
import com.yigongbao.module.design.service.DesignDrawingService;
import com.yigongbao.module.design.service.DesignInstructionService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.service.DesignProductService;
import com.yigongbao.module.design.vo.DesignDocVersionVO;
import com.yigongbao.module.design.vo.DocItemVO;
import com.yigongbao.module.design.vo.GenerateDocsResultVO;
import com.yigongbao.module.order.service.OrderMainService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    private final DesignInstructionService instructionService;
    private final DesignDrawingService drawingService;
    private final InstructionExcelBuilder instructionBuilder;
    private final DrawingExcelBuilder drawingBuilder;
    private final CodeGeneratorService codeGeneratorService;
    private final FileStorageService fileStorageService;

    /**
     * 同时生成指令单和图纸
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 生成结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GenerateDocsResultVO generateDocs(Long orderId, Long packageId) {
        log.info("开始生成指令单和图纸，orderId={}, packageId={}", orderId, packageId);

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

        // 3. 计算新版本号（取指令单和图纸中较大的版本序号 + 1，保持一致）
        int newSeq = Math.max(
                instructionService.getMaxVersionSeq(packageId),
                drawingService.getMaxVersionSeq(packageId)) + 1;
        String version = "A/" + newSeq;
        log.info("新版本号：{}，packageId={}", version, packageId);

        // 4. 查询打印产品列表
        List<DesignProductEntity> products = productService.list(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId)
                        .orderByAsc(DesignProductEntity::getSortOrder));

        // 5. 生成指令单 Excel
        String instructionCode = codeGeneratorService.generate(CodeRuleConstants.INSTRUCTION_NO);
        InstructionExcelBuilder.BuildContext instrCtx = buildInstructionContext(order, pkg, products, version);
        byte[] instrBytes;
        try {
            instrBytes = instructionBuilder.build(instrCtx);
        } catch (IOException e) {
            log.error("生成指令单 Excel 失败，packageId={}", packageId, e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }

        // 6. 生成图纸 Excel
        DrawingExcelBuilder.BuildContext drawCtx = buildDrawingContext(order, pkg, products);
        byte[] drawBytes;
        try {
            drawBytes = drawingBuilder.build(drawCtx);
        } catch (IOException e) {
            log.error("生成图纸 Excel 失败，packageId={}", packageId, e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }

        // 7. 上传文件到存储服务
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        FileInfo instrFile = uploadBytes(instrBytes,
                FileBizTypeEnum.INSTRUCTION_FILE.getCode() + "/" + datePath + "/",
                FileBizTypeEnum.INSTRUCTION_FILE.getDictCode(),
                instructionCode + ".xlsx");
        FileInfo drawFile = uploadBytes(drawBytes,
                FileBizTypeEnum.DRAWING_FILE.getCode() + "/" + datePath + "/",
                FileBizTypeEnum.DRAWING_FILE.getDictCode(),
                pkg.getPackageCode() + "-图纸-" + version + ".xlsx");

        // 8. 插入指令单记录（保留历史版本，不逻辑删除旧记录）
        DesignInstructionEntity instrEntity = new DesignInstructionEntity();
        instrEntity.setOrderId(orderId);
        instrEntity.setPackageId(packageId);
        instrEntity.setInstructionCode(instructionCode);
        instrEntity.setVersion(version);
        instrEntity.setVersionSeq(newSeq);
        instrEntity.setTemplateFileId(instrFile.getId());
        instrEntity.setTemplateFileUrl(instrFile.getUrl());
        instrEntity.setGenerateTime(LocalDateTime.now());
        instructionService.save(instrEntity);

        // 9. 插入图纸记录（保留历史版本，不逻辑删除旧记录）
        DesignDrawingEntity drawEntity = new DesignDrawingEntity();
        drawEntity.setOrderId(orderId);
        drawEntity.setPackageId(packageId);
        drawEntity.setVersion(version);
        drawEntity.setVersionSeq(newSeq);
        drawEntity.setTemplateFileId(drawFile.getId());
        drawEntity.setTemplateFileUrl(drawFile.getUrl());
        drawEntity.setGenerateTime(LocalDateTime.now());
        drawingService.save(drawEntity);

        // 10. 构造返回值
        GenerateDocsResultVO result = new GenerateDocsResultVO();

        DocItemVO instrVO = new DocItemVO();
        instrVO.setId(instrEntity.getId());
        instrVO.setVersion(version);
        instrVO.setTemplateFileUrl(instrFile.getUrl());
        instrVO.setGenerateTime(instrEntity.getGenerateTime());
        result.setInstruction(instrVO);

        DocItemVO drawVO = new DocItemVO();
        drawVO.setId(drawEntity.getId());
        drawVO.setVersion(version);
        drawVO.setTemplateFileUrl(drawFile.getUrl());
        drawVO.setGenerateTime(drawEntity.getGenerateTime());
        result.setDrawing(drawVO);

        log.info("生成指令单和图纸完成，orderId={}, packageId={}, version={}", orderId, packageId, version);
        return result;
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
     * 下载指定版本的指令单（模板版）
     */
    @Override
    public void downloadInstruction(Long orderId, Long packageId, Long id,
                                    HttpServletResponse response) throws IOException {
        log.info("下载指令单，orderId={}, packageId={}, id={}", orderId, packageId, id);
        validatePackage(orderId, packageId);
        DesignInstructionEntity entity = instructionService.getById(id);
        if (entity == null || !entity.getPackageId().equals(packageId)) {
            throw new BusinessException(ErrorCodeEnum.DOC_VERSION_NOT_FOUND);
        }
        downloadFile(entity.getTemplateFileUrl(), entity.getInstructionCode() + ".xlsx", response);
    }

    /**
     * 下载指定版本的图纸（模板版）
     */
    @Override
    public void downloadDrawing(Long orderId, Long packageId, Long id,
                                HttpServletResponse response) throws IOException {
        log.info("下载图纸，orderId={}, packageId={}, id={}", orderId, packageId, id);
        validatePackage(orderId, packageId);
        DesignDrawingEntity entity = drawingService.getById(id);
        if (entity == null || !entity.getPackageId().equals(packageId)) {
            throw new BusinessException(ErrorCodeEnum.DOC_VERSION_NOT_FOUND);
        }
        String filename = "图纸-" + entity.getVersion() + ".xlsx";
        downloadFile(entity.getTemplateFileUrl(), filename, response);
    }

    /**
     * 上传修订版指令单
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
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        FileInfo fileInfo = uploadMultipartFile(file,
                FileBizTypeEnum.INSTRUCTION_FILE.getCode() + "/" + datePath + "/",
                FileBizTypeEnum.INSTRUCTION_FILE.getDictCode());
        entity.setRevisedFileId(fileInfo.getId());
        entity.setRevisedFileUrl(fileInfo.getUrl());
        entity.setRevisedUploadTime(LocalDateTime.now());
        instructionService.updateById(entity);
        log.info("上传修订版指令单成功，id={}", id);
    }

    /**
     * 上传修订版图纸
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
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        FileInfo fileInfo = uploadMultipartFile(file,
                FileBizTypeEnum.DRAWING_FILE.getCode() + "/" + datePath + "/",
                FileBizTypeEnum.DRAWING_FILE.getDictCode());
        entity.setRevisedFileId(fileInfo.getId());
        entity.setRevisedFileUrl(fileInfo.getUrl());
        entity.setRevisedUploadTime(LocalDateTime.now());
        drawingService.updateById(entity);
        log.info("上传修订版图纸成功，id={}", id);
    }

    // ==================== 私有方法 ====================

    /**
     * 校验订单状态和操作权限（设计中或设计审核不通过时，当前用户是订单设计师）
     */
    private OrderMainEntity checkOrderAndPermission(Long orderId) {
        // 查询订单
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        // 校验阶段（必须在设计阶段）
        FlowStatusEnum status = FlowStatusEnum.getByValue(order.getStatus());
        if (status == null || !status.belongsTo(FlowPhaseEnum.DESIGN)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED);
        }
        // 校验状态（设计中或设计审核不通过才能操作）
        if (status != FlowStatusEnum.DESIGN_IN_PROGRESS
                && status != FlowStatusEnum.DESIGN_REVIEW_REJECTED) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED);
        }
        // 校验操作人（必须是当前设计师）
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
            List<DesignProductEntity> products, String version) {
        InstructionExcelBuilder.BuildContext ctx = new InstructionExcelBuilder.BuildContext();
        ctx.setOrderCode(order.getOrderCode());
        ctx.setPatientName(order.getPatientName());
        ctx.setHospitalName(order.getHospitalName());
        ctx.setContactName(""); // OrderMainEntity 暂无联系人字段，由线下填写
        ctx.setPackageCode(pkg.getPackageCode());
        ctx.setVersion(version);
        ctx.setProducts(products);
        // 预交货时间（LocalDateTime 转字符串）
        if (order.getExpectedDeliveryDate() != null) {
            ctx.setExpectedDeliveryDate(order.getExpectedDeliveryDate()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        ctx.setPostalAddress(order.getPostalAddress());
        return ctx;
    }

    /**
     * 构建图纸填充上下文
     */
    private DrawingExcelBuilder.BuildContext buildDrawingContext(
            OrderMainEntity order, DesignPackageEntity pkg,
            List<DesignProductEntity> products) {
        DrawingExcelBuilder.BuildContext ctx = new DrawingExcelBuilder.BuildContext();
        ctx.setOrderCode(order.getOrderCode());
        ctx.setPackageCode(pkg.getPackageCode());
        ctx.setProducts(products);
        return ctx;
    }

    /**
     * 将 byte[] 上传到 x-file-storage
     *
     * @param bytes      文件字节数组
     * @param path       存储路径
     * @param objectType 业务类型编码
     * @param filename   文件名
     * @return 上传结果 FileInfo
     */
    protected FileInfo uploadBytes(byte[] bytes, String path, String objectType, String filename) {
        try {
            return fileStorageService.of(bytes)
                    .setPath(path)
                    .setObjectType(objectType)
                    .setOriginalFilename(filename)
                    .upload();
        } catch (Exception e) {
            log.error("上传文件失败，filename={}", filename, e);
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_UPLOAD_FAILED);
        }
    }

    /**
     * 将 MultipartFile 上传到 x-file-storage
     *
     * @param file       修订版文件
     * @param path       存储路径
     * @param objectType 业务类型编码
     * @return 上传结果 FileInfo
     */
    protected FileInfo uploadMultipartFile(MultipartFile file, String path, String objectType) {
        try {
            return fileStorageService.of(file)
                    .setPath(path)
                    .setObjectType(objectType)
                    .upload();
        } catch (Exception e) {
            log.error("上传修订版文件失败", e);
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_UPLOAD_FAILED);
        }
    }

    /**
     * 通过 URL 直接流式下载文件到响应
     */
    private void downloadFile(String fileUrl, String filename, HttpServletResponse response) throws IOException {
        if (fileUrl == null) {
            throw new BusinessException(ErrorCodeEnum.DOC_VERSION_NOT_FOUND);
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");
        try (InputStream is = new URL(fileUrl).openStream()) {
            is.transferTo(response.getOutputStream());
        }
    }

    private DesignDocVersionVO toInstructionVersionVO(DesignInstructionEntity entity) {
        DesignDocVersionVO vo = new DesignDocVersionVO();
        vo.setId(entity.getId());
        vo.setVersion(entity.getVersion());
        vo.setVersionSeq(entity.getVersionSeq());
        vo.setTemplateFileUrl(entity.getTemplateFileUrl());
        vo.setRevisedFileUrl(entity.getRevisedFileUrl());
        vo.setGenerateTime(entity.getGenerateTime());
        vo.setRevisedUploadTime(entity.getRevisedUploadTime());
        return vo;
    }

    private DesignDocVersionVO toDrawingVersionVO(DesignDrawingEntity entity) {
        DesignDocVersionVO vo = new DesignDocVersionVO();
        vo.setId(entity.getId());
        vo.setVersion(entity.getVersion());
        vo.setVersionSeq(entity.getVersionSeq());
        vo.setTemplateFileUrl(entity.getTemplateFileUrl());
        vo.setRevisedFileUrl(entity.getRevisedFileUrl());
        vo.setGenerateTime(entity.getGenerateTime());
        vo.setRevisedUploadTime(entity.getRevisedUploadTime());
        return vo;
    }
}
