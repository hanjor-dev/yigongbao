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
import com.yigongbao.module.design.helper.DrawingExcelBuilder;
import com.yigongbao.module.design.helper.InstructionExcelBuilder;
import com.yigongbao.module.design.service.DesignDocService;
import com.yigongbao.module.design.service.DesignDrawingService;
import com.yigongbao.module.design.service.DesignInstructionService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.service.DesignProductService;
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
    private final FileService fileService;

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
        // 最新版已封版（上传过修订版）或无历史版本，则新建；否则覆盖
        boolean isNewVersion = (latest == null || latest.getRevisedFileId() != null);
        int newSeq = isNewVersion ? (latest == null ? 1 : latest.getVersionSeq() + 1)
                : latest.getVersionSeq();
        String version = "A/" + newSeq;
        log.info("指令单版本：{}，isNewVersion={}，packageId={}", version, isNewVersion, packageId);

        // 4. 查询打印产品列表
        List<DesignProductEntity> products = productService.list(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId)
                        .orderByAsc(DesignProductEntity::getSortOrder));

        // 5. 生成指令单 Excel
        String instructionCode = isNewVersion
                ? codeGeneratorService.generate(CodeRuleConstants.INSTRUCTION_NO)
                : latest.getInstructionCode();
        InstructionExcelBuilder.BuildContext instrCtx = buildInstructionContext(order, pkg, products, version);
        byte[] instrBytes;
        try {
            instrBytes = instructionBuilder.build(instrCtx);
        } catch (IOException e) {
            log.error("生成指令单 Excel 失败，packageId={}", packageId, e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }

        // 6. 上传文件
        String filename = instructionCode + ".xlsx";
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
            instrEntity.setGenerateTime(LocalDateTime.now());
            instrEntity.setTemplateFileId(instrFile.getId());
            instrEntity.setTemplateFileUrl(instrFile.getFileUrl());
            instructionService.save(instrEntity);
        } else {
            // 覆盖当前版本的模板文件
            latest.setTemplateFileId(instrFile.getId());
            latest.setTemplateFileUrl(instrFile.getFileUrl());
            latest.setGenerateTime(LocalDateTime.now());
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

        // 4. 查询打印产品列表
        List<DesignProductEntity> products = productService.list(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId)
                        .orderByAsc(DesignProductEntity::getSortOrder));

        // 5. 生成图纸 Excel
        DrawingExcelBuilder.BuildContext drawCtx = buildDrawingContext(order, pkg, products);
        byte[] drawBytes;
        try {
            drawBytes = drawingBuilder.build(drawCtx);
        } catch (IOException e) {
            log.error("生成图纸 Excel 失败，packageId={}", packageId, e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }

        // 6. 上传文件
        String filename = pkg.getPackageCode() + "-图纸-" + version + ".xlsx";
        FileVO drawFile = fileService.uploadBytes(drawBytes, filename, FileBizTypeEnum.DRAWING_FILE.getDictCode());

        // 7. 新建记录或更新模板文件
        DesignDrawingEntity drawEntity;
        if (isNewVersion) {
            drawEntity = new DesignDrawingEntity();
            drawEntity.setOrderId(orderId);
            drawEntity.setPackageId(packageId);
            drawEntity.setVersion(version);
            drawEntity.setVersionSeq(newSeq);
            drawEntity.setGenerateTime(LocalDateTime.now());
            drawEntity.setTemplateFileId(drawFile.getId());
            drawEntity.setTemplateFileUrl(drawFile.getFileUrl());
            drawingService.save(drawEntity);
        } else {
            latest.setTemplateFileId(drawFile.getId());
            latest.setTemplateFileUrl(drawFile.getFileUrl());
            latest.setGenerateTime(LocalDateTime.now());
            drawingService.updateById(latest);
            drawEntity = latest;
        }

        // 8. 构造返回值
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
        entity.setRevisedFileId(fileVO.getId());
        entity.setRevisedFileUrl(fileVO.getFileUrl());
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
        FileVO fileVO = fileService.uploadFile(file, FileBizTypeEnum.DRAWING_FILE.getDictCode());
        entity.setRevisedFileId(fileVO.getId());
        entity.setRevisedFileUrl(fileVO.getFileUrl());
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
        ctx.setContactName(order.getDoctorName());
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
        return vo;
    }
}
