package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.FileBizTypeEnum;
import com.yigongbao.common.exception.BusinessException;
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
import com.yigongbao.module.design.mapper.DesignPackageFileScreenshotMapper;
import com.yigongbao.module.design.mapper.DesignProductMapper;
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
import jakarta.servlet.http.HttpServletResponse;
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
 * 指令单/图纸管理服务实现类
 * <p>
 * 生成逻辑已内化为"按需自动生成"（ensureInstruction / ensureDrawing）：
 * - 首次调用或打印信息变化后：重新生成 Excel，覆盖或新建版本记录，is_confirmed 重置为 0
 * - 打印信息未变化：直接复用已有版本记录，不产生新文件，不改变 is_confirmed
 * </p>
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
    private final DesignProductMapper designProductMapper;
    private final DesignProductFileService productFileService;
    private final DesignInstructionService instructionService;
    private final DesignDrawingService drawingService;
    private final InstructionExcelBuilder instructionBuilder;
    private final DrawingExcelBuilder drawingBuilder;
    private final CodeGeneratorService codeGeneratorService;
    private final FileService fileService;
    private final DesignScreenshotService screenshotService;
    private final DesignPackageFileScreenshotMapper screenshotMapper;
    private final com.yigongbao.module.design.helper.DesignQueryHelper designQueryHelper;

    // ==================== 线下模式：下载接口 ====================

    /**
     * 下载指令单模板（线下模式）
     * <p>
     * 按需自动生成后流式返回文件，调用方无需提前调用"生成"接口。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void downloadInstruction(Long orderId, Long packageId, HttpServletResponse response) {
        checkDesignPhase(orderId);
        // 按需生成或复用已有版本
        DesignInstructionEntity entity = ensureInstruction(orderId, packageId);
        // 流式下载模板文件
        try {
            fileService.download(entity.getTemplateFileId(), response);
        } catch (IOException e) {
            log.error("指令单模板下载失败: packageId={}, templateFileId={}", packageId, entity.getTemplateFileId(), e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }
    }

    /**
     * 下载图纸模板（线下模式）
     * <p>
     * 按需自动生成后流式返回文件，调用方无需提前调用"生成"接口。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void downloadDrawing(Long orderId, Long packageId, HttpServletResponse response) {
        log.info("下载图纸模板，orderId={}, packageId={}", orderId, packageId);
        checkDesignPhase(orderId);
        // 按需生成或复用已有版本
        DesignDrawingEntity entity = ensureDrawing(orderId, packageId);
        // 流式下载模板文件
        try {
            fileService.download(entity.getTemplateFileId(), response);
        } catch (IOException e) {
            log.error("图纸模板下载失败，packageId={}, templateFileId={}", packageId, entity.getTemplateFileId(), e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }
        log.info("图纸模板下载完成，packageId={}, version={}", packageId, entity.getVersion());
    }

    // ==================== 在线模式：预览 URL 接口 ====================

    /**
     * 获取指令单预览 URL（在线模式）
     * <p>
     * 按需自动生成后返回文件 URL 和当前确认状态，前端据此渲染 Viewer 并决定是否展示确认按钮。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocItemVO getInstructionPreviewUrl(Long orderId, Long packageId) {
        log.info("获取指令单预览URL，orderId={}, packageId={}", orderId, packageId);
        checkDesignPhase(orderId);
        // 按需生成或复用已有版本
        DesignInstructionEntity entity = ensureInstruction(orderId, packageId);
        // 构造返回 VO
        DocItemVO vo = toInstructionDocItemVO(entity);
        log.info("获取指令单预览URL完成，packageId={}, version={}, isConfirmed={}", packageId, entity.getVersion(), entity.getIsConfirmed());
        return vo;
    }

    /**
     * 获取图纸预览 URL（在线模式）
     * <p>
     * 按需自动生成后返回文件 URL 和当前确认状态，前端据此渲染 Viewer 并决定是否展示确认按钮。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocItemVO getDrawingPreviewUrl(Long orderId, Long packageId) {
        log.info("获取图纸预览URL，orderId={}, packageId={}", orderId, packageId);
        checkDesignPhase(orderId);
        // 按需生成或复用已有版本
        DesignDrawingEntity entity = ensureDrawing(orderId, packageId);
        // 构造返回 VO
        DocItemVO vo = toDrawingDocItemVO(entity);
        log.info("获取图纸预览URL完成，packageId={}, version={}, isConfirmed={}", packageId, entity.getVersion(), entity.getIsConfirmed());
        return vo;
    }

    // ==================== 版本历史查询 ====================

    /**
     * 查询指令单版本历史列表
     */
    @Override
    public List<DesignDocVersionVO> listInstructionVersions(Long orderId, Long packageId) {
        log.info("查询指令单版本列表，orderId={}, packageId={}", orderId, packageId);
        checkDesignPhase(orderId);
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
        checkDesignPhase(orderId);
        validatePackage(orderId, packageId);
        return drawingService.listVersions(packageId).stream()
                .map(this::toDrawingVersionVO)
                .collect(Collectors.toList());
    }

    // ==================== 修订版上传 ====================

    /**
     * 上传修订版指令单
     * <p>
     * 创建新版本记录，sourceType = 'MANUAL'，上传后自动将 is_confirmed 置为 1。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadRevisedInstruction(Long orderId, Long packageId, Long id, MultipartFile file) {
        log.info("上传修订版指令单，orderId={}, packageId={}, id={}", orderId, packageId, id);
        checkDesignPhase(orderId);
        validatePackage(orderId, packageId);

        // 查询当前最新版本
        DesignInstructionEntity latest = instructionService.getLatestVersion(packageId);
        if (latest == null) {
            throw new BusinessException(ErrorCodeEnum.DOC_VERSION_NOT_FOUND);
        }

        // 上传文件
        FileVO fileVO = fileService.uploadFile(file, FileBizTypeEnum.INSTRUCTION_FILE.getDictCode());
        LocalDateTime uploadTime = LocalDateTime.now();

        // 创建新版本记录
        int newVersionSeq = latest.getVersionSeq() + 1;
        String newVersion = "A/" + newVersionSeq;
        String newInstructionCode = codeGeneratorService.generate(CodeRuleConstants.INSTRUCTION_NO);

        DesignInstructionEntity newEntity = new DesignInstructionEntity();
        newEntity.setOrderId(orderId);
        newEntity.setPackageId(packageId);
        newEntity.setInstructionCode(newInstructionCode);
        newEntity.setVersion(newVersion);
        newEntity.setVersionSeq(newVersionSeq);
        newEntity.setSourceType(StatusConstants.SOURCE_TYPE_MANUAL);
        newEntity.setTemplateFileId(fileVO.getId());
        newEntity.setTemplateFileUrl(fileVO.getFileUrl());
        newEntity.setGenerateTime(uploadTime);
        newEntity.setIsConfirmed(StatusConstants.CONFIRMED);
        newEntity.setConfirmTime(uploadTime);

        instructionService.save(newEntity);
        log.info("上传修订版指令单成功，创建新版本，packageId={}, version={}, instructionCode={}", packageId, newVersion, newInstructionCode);
    }

    /**
     * 上传修订版图纸
     * <p>
     * 创建新版本记录，sourceType = 'MANUAL'，上传后自动将 is_confirmed 置为 1。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadRevisedDrawing(Long orderId, Long packageId, Long id, MultipartFile file) {
        log.info("上传修订版图纸，orderId={}, packageId={}, id={}", orderId, packageId, id);
        checkDesignPhase(orderId);
        validatePackage(orderId, packageId);

        // 查询当前最新版本
        DesignDrawingEntity latest = drawingService.getLatestVersion(packageId);
        if (latest == null) {
            throw new BusinessException(ErrorCodeEnum.DOC_VERSION_NOT_FOUND);
        }

        // 上传文件
        FileVO fileVO = fileService.uploadFile(file, FileBizTypeEnum.DRAWING_FILE.getDictCode());
        LocalDateTime uploadTime = LocalDateTime.now();

        // 创建新版本记录
        int newVersionSeq = latest.getVersionSeq() + 1;
        String newVersion = "A/" + newVersionSeq;

        DesignDrawingEntity newEntity = new DesignDrawingEntity();
        newEntity.setOrderId(orderId);
        newEntity.setPackageId(packageId);
        newEntity.setVersion(newVersion);
        newEntity.setVersionSeq(newVersionSeq);
        newEntity.setSourceType(StatusConstants.SOURCE_TYPE_MANUAL);
        newEntity.setTemplateFileId(fileVO.getId());
        newEntity.setTemplateFileUrl(fileVO.getFileUrl());
        newEntity.setGenerateTime(uploadTime);
        newEntity.setIsConfirmed(StatusConstants.CONFIRMED);
        newEntity.setConfirmTime(uploadTime);

        drawingService.save(newEntity);
        log.info("上传修订版图纸成功，创建新版本，packageId={}, version={}", packageId, newVersion);
    }

    // ==================== 确认接口 ====================

    /**
     * 确认图纸（在线模式）
     * <p>
     * 设计师预览生成的图纸满意后调用，将 is_confirmed 置为 1。
     * 若之后打印信息变化触发重新生成，is_confirmed 会被自动重置为 0。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDrawing(Long orderId, Long packageId, Long id) {
        log.info("确认图纸，orderId={}, packageId={}, id={}", orderId, packageId, id);
        checkDesignPhase(orderId);
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
     * 确认指令单（在线模式）
     * <p>
     * 设计师确认生成的指令单内容无误后调用，将 is_confirmed 置为 1。
     * 若之后打印信息变化触发重新生成，is_confirmed 会被自动重置为 0。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmInstruction(Long orderId, Long packageId, Long id) {
        log.info("确认指令单，orderId={}, packageId={}, id={}", orderId, packageId, id);
        checkDesignPhase(orderId);
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

    // ==================== 批量查询（供工单详情页使用） ====================

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

    // ==================== 核心私有方法：按需生成 ====================

    /**
     * 按需确保指令单存在且内容最新（幂等）
     * <p>
     * 决策逻辑：
     * <ol>
     *   <li>无版本记录（首次）→ 生成，创建 A/1</li>
     *   <li>有记录且未封版（revisedFileId==null）且打印信息已变化 → 重新生成，覆盖当前版本，is_confirmed 重置</li>
     *   <li>有记录且已封版（revisedFileId!=null）且打印信息已变化 → 生成，新建下一版本</li>
     *   <li>有记录且打印信息未变化 → 直接复用，不重新生成</li>
     * </ol>
     * </p>
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 有效的 DesignInstructionEntity（已持久化）
     */
    private DesignInstructionEntity ensureInstruction(Long orderId, Long packageId) {
        log.info("按需确保指令单有效，orderId={}, packageId={}", orderId, packageId);

        // 加载基础数据
        OrderMainEntity order = orderMainService.getById(orderId);
        DesignPackageEntity pkg = validatePackage(orderId, packageId);

        // 前置校验：打印信息已填写
        long productCount = productService.count(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId));
        if (productCount == 0) {
            throw new BusinessException(ErrorCodeEnum.PRINT_INFO_REQUIRED);
        }

        // 查询最新版本
        DesignInstructionEntity latest = instructionService.getLatestVersion(packageId);

        // 判断打印信息是否在上次生成后发生变化
        boolean dataChanged = isPrintInfoChangedSince(packageId,
                latest != null ? latest.getGenerateTime() : null);

        if (latest == null) {
            // 场景1：首次，生成 A/1
            log.info("指令单首次生成，packageId={}", packageId);
            return doGenerateInstruction(order, pkg, null, 1);
        }

        if (!dataChanged) {
            // 场景4：打印信息未变，直接复用
            log.info("打印信息未变化，复用已有指令单，packageId={}, version={}", packageId, latest.getVersion());
            return latest;
        }

        // 打印信息已变化
        if (!StatusConstants.SOURCE_TYPE_MANUAL.equals(latest.getSourceType())) {
            // 场景2：未封版（自动生成的版本），覆盖当前版本
            log.info("打印信息已变化，覆盖当前指令单版本，packageId={}, version={}", packageId, latest.getVersion());
            return doGenerateInstruction(order, pkg, latest, latest.getVersionSeq());
        } else {
            // 场景3：已封版（手动上传的版本），新建下一版本
            log.info("打印信息已变化，新建下一指令单版本，packageId={}, prevVersion={}", packageId, latest.getVersion());
            return doGenerateInstruction(order, pkg, null, latest.getVersionSeq() + 1);
        }
    }

    /**
     * 按需确保图纸存在且内容最新（幂等）
     * <p>
     * 决策逻辑同 ensureInstruction。
     * </p>
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 有效的 DesignDrawingEntity（已持久化）
     */
    private DesignDrawingEntity ensureDrawing(Long orderId, Long packageId) {
        log.info("按需确保图纸有效，orderId={}, packageId={}", orderId, packageId);

        // 加载基础数据
        OrderMainEntity order = orderMainService.getById(orderId);
        DesignPackageEntity pkg = validatePackage(orderId, packageId);

        // 前置校验：打印信息已填写
        long productCount = productService.count(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId));
        if (productCount == 0) {
            throw new BusinessException(ErrorCodeEnum.PRINT_INFO_REQUIRED);
        }

        // 查询最新版本
        DesignDrawingEntity latest = drawingService.getLatestVersion(packageId);

        // 判断打印信息是否在上次生成后发生变化
        boolean dataChanged = isPrintInfoChangedSince(packageId,
                latest != null ? latest.getGenerateTime() : null);

        if (latest == null) {
            // 场景1：首次，生成 A/1
            log.info("图纸首次生成，packageId={}", packageId);
            return doGenerateDrawing(order, pkg, null, 1);
        }

        if (!dataChanged) {
            // 场景4：打印信息未变，直接复用
            log.info("打印信息未变化，复用已有图纸，packageId={}, version={}", packageId, latest.getVersion());
            return latest;
        }

        // 打印信息已变化
        if (!StatusConstants.SOURCE_TYPE_MANUAL.equals(latest.getSourceType())) {
            // 场景2：未封版（自动生成的版本），覆盖当前版本
            log.info("打印信息已变化，覆盖当前图纸版本，packageId={}, version={}", packageId, latest.getVersion());
            return doGenerateDrawing(order, pkg, latest, latest.getVersionSeq());
        } else {
            // 场景3：已封版（手动上传的版本），新建下一版本
            log.info("打印信息已变化，新建下一图纸版本，packageId={}, prevVersion={}", packageId, latest.getVersion());
            return doGenerateDrawing(order, pkg, null, latest.getVersionSeq() + 1);
        }
    }

    /**
     * 判断指定数据包的打印信息是否在给定时间之后发生过变化
     * <p>
     * 同时检查产品行（design_product.update_time）和包级字段（design_package.update_time），
     * 取两者最大值与 sinceTime 比较。
     * </p>
     *
     * @param packageId 数据包ID
     * @param sinceTime 上次生成时间；为 null 时返回 true（视为首次，需要生成）
     * @return true=打印信息有变化或从未生成，false=无变化
     */
    private boolean isPrintInfoChangedSince(Long packageId, LocalDateTime sinceTime) {
        if (sinceTime == null) {
            return true;
        }
        // 产品行最后修改时间
        LocalDateTime productUpdateTime = designProductMapper.getLatestUpdateTime(packageId);
        // 包级字段最后修改时间（productMark/packQuantity/remark 也影响指令单内容）
        DesignPackageEntity pkg = packageService.getById(packageId);
        LocalDateTime pkgUpdateTime = pkg != null ? pkg.getUpdateTime() : null;
        // 截图最后修改时间（saveMark 后应触发重新生成）
        LocalDateTime screenshotUpdateTime = screenshotMapper.getLatestUpdateTime(packageId);

        // 取三者最大值
        LocalDateTime dataLastModified = laterOf(laterOf(productUpdateTime, pkgUpdateTime), screenshotUpdateTime);
        if (dataLastModified == null) {
            // 无打印信息记录（不应发生，已在调用方校验了 productCount > 0）
            return true;
        }
        return dataLastModified.isAfter(sinceTime);
    }

    /**
     * 返回两个时间中较晚的一个，任一为 null 时返回另一个
     */
    private static LocalDateTime laterOf(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    /**
     * 执行指令单生成：填充 Excel → 上传 OSS → 持久化记录
     *
     * @param order      订单实体（提供表头信息）
     * @param pkg        数据包实体（提供包编号等信息）
     * @param toOverride 非 null 时覆盖该记录（场景2）；null 时新建记录（场景1/3）
     * @param versionSeq 目标版本序号
     * @return 已持久化的 DesignInstructionEntity
     */
    protected DesignInstructionEntity doGenerateInstruction(
            OrderMainEntity order, DesignPackageEntity pkg,
            DesignInstructionEntity toOverride, int versionSeq) {

        Long packageId = pkg.getId();
        String version = "A/" + versionSeq;
        LocalDateTime now = LocalDateTime.now();

        // 查询打印产品列表并展开为行
        List<DesignProductEntity> products = productService.list(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId)
                        .orderByAsc(DesignProductEntity::getSortOrder));
        List<InstructionExcelBuilder.ProductRow> rows = expandProductRows(products);

        // 生成指令单 Excel
        InstructionExcelBuilder.BuildContext ctx = buildInstructionContext(order, pkg, rows, version, now);
        byte[] bytes;
        try {
            bytes = instructionBuilder.build(ctx);
        } catch (IOException e) {
            log.error("生成指令单 Excel 失败，packageId={}", packageId, e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }

        // 上传 OSS
        String filename = "指令单-" + pkg.getPackageCode() + ".xlsx";
        FileVO fileVO = fileService.uploadBytes(bytes, filename, FileBizTypeEnum.INSTRUCTION_FILE.getDictCode());

        // 确定指令单编号（覆盖时复用已有编号，新建时生成新编号）
        String instructionCode = (toOverride != null)
                ? toOverride.getInstructionCode()
                : codeGeneratorService.generate(CodeRuleConstants.INSTRUCTION_NO);

        // 持久化
        if (toOverride != null) {
            // 覆盖现有版本，先记录旧文件ID以便删除
            String oldTemplateFileId = toOverride.getTemplateFileId();
            toOverride.setTemplateFileId(fileVO.getId());
            toOverride.setTemplateFileUrl(fileVO.getFileUrl());
            toOverride.setGenerateTime(now);
            toOverride.setSourceType(StatusConstants.SOURCE_TYPE_AUTO);
            // 数据已变，重置确认状态，强制重新确认
            toOverride.setIsConfirmed(StatusConstants.NOT_CONFIRMED);
            toOverride.setConfirmTime(null);
            instructionService.updateById(toOverride);
            // 删除旧模板文件，避免 OSS 泄漏
            if (oldTemplateFileId != null) {
                fileService.deleteById(oldTemplateFileId);
            }
            log.info("指令单覆盖完成，packageId={}, version={}", packageId, version);
            return toOverride;
        } else {
            // 新建版本
            DesignInstructionEntity entity = new DesignInstructionEntity();
            entity.setOrderId(order.getId());
            entity.setPackageId(packageId);
            entity.setInstructionCode(instructionCode);
            entity.setVersion(version);
            entity.setVersionSeq(versionSeq);
            entity.setSourceType("AUTO");
            entity.setGenerateTime(now);
            entity.setTemplateFileId(fileVO.getId());
            entity.setTemplateFileUrl(fileVO.getFileUrl());
            // 新版本初始为未确认状态
            entity.setIsConfirmed(StatusConstants.NOT_CONFIRMED);
            instructionService.save(entity);
            log.info("指令单新建完成，packageId={}, version={}", packageId, version);
            return entity;
        }
    }

    /**
     * 执行图纸生成：填充 Excel → 上传 OSS → 持久化记录
     *
     * @param order      订单实体
     * @param pkg        数据包实体
     * @param toOverride 非 null 时覆盖该记录（场景2）；null 时新建记录（场景1/3）
     * @param versionSeq 目标版本序号
     * @return 已持久化的 DesignDrawingEntity
     */
    protected DesignDrawingEntity doGenerateDrawing(
            OrderMainEntity order, DesignPackageEntity pkg,
            DesignDrawingEntity toOverride, int versionSeq) {

        Long packageId = pkg.getId();
        String version = "A/" + versionSeq;
        LocalDateTime now = LocalDateTime.now();

        // 查询产品列表和关联文件
        List<DesignProductEntity> products = productService.list(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId)
                        .orderByAsc(DesignProductEntity::getSortOrder));
        List<Long> productIds = products.stream().map(DesignProductEntity::getId).toList();
        List<DesignProductFileEntity> allProductFiles = productFileService.listByProductIds(productIds);

        // 批量加载截图字节
        List<Long> packageFileIds = allProductFiles.stream()
                .map(DesignProductFileEntity::getPackageFileId).distinct().toList();
        Map<Long, String> screenshotFileIdMap = screenshotService.listFileIdsByPackageFileIds(packageFileIds);
        Map<Long, byte[]> screenshotBytesMap = loadScreenshotBytes(screenshotFileIdMap);

        List<DrawingExcelBuilder.ProductRow> drawingRows = expandDrawingRows(products, allProductFiles, screenshotBytesMap);

        // 生成图纸 Excel
        DrawingExcelBuilder.BuildContext ctx = buildDrawingContext(order, pkg, drawingRows, now);
        byte[] bytes;
        try {
            bytes = drawingBuilder.build(ctx);
        } catch (IOException e) {
            log.error("生成图纸 Excel 失败，packageId={}", packageId, e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }

        // 上传 OSS
        String filename = "图纸-" + pkg.getPackageCode() + ".xlsx";
        FileVO fileVO = fileService.uploadBytes(bytes, filename, FileBizTypeEnum.DRAWING_FILE.getDictCode());

        // 持久化
        if (toOverride != null) {
            // 覆盖现有版本，先记录旧文件ID以便删除
            String oldTemplateFileId = toOverride.getTemplateFileId();
            toOverride.setTemplateFileId(fileVO.getId());
            toOverride.setTemplateFileUrl(fileVO.getFileUrl());
            toOverride.setGenerateTime(now);
            toOverride.setSourceType(StatusConstants.SOURCE_TYPE_AUTO);
            // 数据已变，重置确认状态，强制重新确认
            toOverride.setIsConfirmed(StatusConstants.NOT_CONFIRMED);
            toOverride.setConfirmTime(null);
            drawingService.updateById(toOverride);
            // 删除旧模板文件，避免 OSS 泄漏
            if (oldTemplateFileId != null) {
                fileService.deleteById(oldTemplateFileId);
            }
            log.info("图纸覆盖完成，packageId={}, version={}", packageId, version);
            return toOverride;
        } else {
            // 新建版本
            DesignDrawingEntity entity = new DesignDrawingEntity();
            entity.setOrderId(order.getId());
            entity.setPackageId(packageId);
            entity.setVersion(version);
            entity.setVersionSeq(versionSeq);
            entity.setSourceType("AUTO");
            entity.setGenerateTime(now);
            entity.setTemplateFileId(fileVO.getId());
            entity.setTemplateFileUrl(fileVO.getFileUrl());
            // 新版本初始为未确认状态
            entity.setIsConfirmed(StatusConstants.NOT_CONFIRMED);
            drawingService.save(entity);
            log.info("图纸新建完成，packageId={}, version={}", packageId, version);
            return entity;
        }
    }

    // ==================== 私有工具方法 ====================

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
                row.setIsUrgent(p.getIsUrgent());
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
     * 校验订单存在且处于可操作的设计阶段（委托 DesignQueryHelper）
     */
    private OrderMainEntity checkDesignPhase(Long orderId) {
        return designQueryHelper.checkDesignPhase(orderId);
    }

    /**
     * 校验当前用户是该订单的指定设计师（委托 DesignQueryHelper）
     */
    private void checkIsAssignedDesigner(OrderMainEntity order) {
        designQueryHelper.checkIsAssignedDesigner(order);
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
        ctx.setOrgName(order.getOrgName());
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

    /** DesignInstructionEntity → DocItemVO */
    private DocItemVO toInstructionDocItemVO(DesignInstructionEntity entity) {
        DocItemVO vo = new DocItemVO();
        vo.setId(entity.getId());
        vo.setVersion(entity.getVersion());
        vo.setFileId(entity.getTemplateFileId());
        vo.setTemplateFileUrl(entity.getTemplateFileUrl());
        vo.setGenerateTime(entity.getGenerateTime());
        vo.setIsConfirmed(entity.getIsConfirmed());
        return vo;
    }

    /** DesignDrawingEntity → DocItemVO */
    private DocItemVO toDrawingDocItemVO(DesignDrawingEntity entity) {
        DocItemVO vo = new DocItemVO();
        vo.setId(entity.getId());
        vo.setVersion(entity.getVersion());
        vo.setFileId(entity.getTemplateFileId());
        vo.setTemplateFileUrl(entity.getTemplateFileUrl());
        vo.setGenerateTime(entity.getGenerateTime());
        vo.setIsConfirmed(entity.getIsConfirmed());
        return vo;
    }

    private DesignDocVersionVO toInstructionVersionVO(DesignInstructionEntity entity) {
        DesignDocVersionVO vo = new DesignDocVersionVO();
        vo.setId(entity.getId());
        vo.setVersion(entity.getVersion());
        vo.setVersionSeq(entity.getVersionSeq());
        vo.setSourceType(entity.getSourceType());
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
        vo.setSourceType(entity.getSourceType());
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
}
