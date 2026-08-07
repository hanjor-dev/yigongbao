package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.FileBizTypeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
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
import com.yigongbao.module.system.config.service.ConfigService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final String QR_SOURCE_FRONTEND_FILE = "FRONTEND_FILE";
    private static final String QR_SOURCE_BACKEND_FALLBACK = "BACKEND_FALLBACK";

    private record DrawingDownload(DesignDrawingEntity entity, OrderMainEntity order) {
    }

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
    private final ConfigService configService;
    private final TransactionTemplate transactionTemplate;
    /** 进程内按数据包串行生成，避免 preview/download 并发产生重复版本和文件。 */
    private static final ConcurrentHashMap<String, Object> DRAWING_LOCKS = new ConcurrentHashMap<>();

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
    public void downloadDrawing(Long orderId, Long packageId, HttpServletResponse response) {
        downloadDrawing(orderId, packageId, null, response);
    }

    @Override
    public void downloadDrawing(Long orderId, Long packageId, String productCategory, HttpServletResponse response) {
        log.info("下载图纸模板，orderId={}, packageId={}", orderId, packageId);
        // 图纸准备在短事务内完成；事务提交释放行锁后再开始文件流传输。
        DrawingDownload download = transactionTemplate.execute(status -> {
            // 必须在事务内其他数据库读取之前加锁，避免 MySQL RR 快照读取到等待前的旧版本。
            lockPackageForDrawingMutation(orderId, packageId);
            OrderMainEntity order = checkDesignPhase(orderId);
            DesignDrawingEntity entity = ensureDrawing(orderId, packageId, productCategory);
            return new DrawingDownload(entity, order);
        });
        String filename = buildDrawingFilename(download.order(), download.entity().getProductCategory());
        // 流式下载模板文件
        try {
            fileService.download(download.entity().getTemplateFileId(), filename, response);
        } catch (IOException e) {
            log.error("图纸模板下载失败，packageId={}, templateFileId={}",
                    packageId, download.entity().getTemplateFileId(), e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }
        log.info("图纸模板下载完成，packageId={}, version={}", packageId, download.entity().getVersion());
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
        return getDrawingPreviewUrl(orderId, packageId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocItemVO getDrawingPreviewUrl(Long orderId, Long packageId, String productCategory) {
        log.info("获取图纸预览URL，orderId={}, packageId={}", orderId, packageId);
        // 必须在事务内其他数据库读取之前加锁，避免 MySQL RR 快照读取到等待前的旧版本。
        lockPackageForDrawingMutation(orderId, packageId);
        checkDesignPhase(orderId);
        // 按需生成或复用已有版本
        DesignDrawingEntity entity = ensureDrawing(orderId, packageId, productCategory);
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
        return listDrawingVersions(orderId, packageId, null);
    }

    @Override
    public List<DesignDocVersionVO> listDrawingVersions(Long orderId, Long packageId, String productCategory) {
        log.info("查询图纸版本列表，orderId={}, packageId={}", orderId, packageId);
        checkDesignPhase(orderId);
        validatePackage(orderId, packageId);
        String category = resolveCategory(packageId, productCategory);
        return (category == null ? drawingService.listVersions(packageId) : drawingService.listVersions(packageId, category)).stream()
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
        // 经典案例保护：经典案例订单不允许上传新的指令单文件
        orderMainService.checkNotClassicCase(orderId, "上传指令单");
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
        uploadRevisedDrawing(orderId, packageId, null, id, file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadRevisedDrawing(Long orderId, Long packageId, String productCategory, Long id, MultipartFile file) {
        log.info("上传修订版图纸，orderId={}, packageId={}, id={}", orderId, packageId, id);
        // 必须在事务内其他数据库读取之前加锁，等待后再建立一致性读快照。
        lockPackageForDrawingMutation(orderId, packageId);
        // 经典案例保护：经典案例订单不允许上传新的图纸文件
        orderMainService.checkNotClassicCase(orderId, "上传图纸");
        checkDesignPhase(orderId);

        // 查询当前最新版本
        String category = resolveCategory(packageId, productCategory);
        DesignDrawingEntity latest = category == null ? drawingService.getLatestVersion(packageId)
                : drawingService.getLatestVersion(packageId, category);
        if (latest == null || !Objects.equals(latest.getId(), id)
                || !Objects.equals(latest.getPackageId(), packageId)
                || (category != null && !Objects.equals(latest.getProductCategory(), category))) {
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
        newEntity.setProductCategory(category);
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
        confirmDrawing(orderId, packageId, null, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDrawing(Long orderId, Long packageId, String productCategory, Long id) {
        log.info("确认图纸，orderId={}, packageId={}, id={}", orderId, packageId, id);
        // 必须在事务内其他数据库读取之前加锁，等待后再建立一致性读快照。
        lockPackageForDrawingMutation(orderId, packageId);
        checkDesignPhase(orderId);
        String category = resolveCategory(packageId, productCategory);
        DesignDrawingEntity latest = category == null ? drawingService.getLatestVersion(packageId)
                : drawingService.getLatestVersion(packageId, category);
        if (latest == null || !Objects.equals(latest.getId(), id)
                || !Objects.equals(latest.getPackageId(), packageId)
                || (category != null && !Objects.equals(latest.getProductCategory(), category))) {
            throw new BusinessException(ErrorCodeEnum.DOC_VERSION_NOT_FOUND);
        }
        latest.setIsConfirmed(StatusConstants.CONFIRMED);
        latest.setConfirmTime(LocalDateTime.now());
        drawingService.updateById(latest);
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

    @Override
    public Map<Long, List<DesignDocVersionVO>> getLatestDrawingGroups(Collection<Long> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) return Collections.emptyMap();
        List<DesignProductEntity> products = productService.list(new LambdaQueryWrapper<DesignProductEntity>()
                .in(DesignProductEntity::getPackageId, packageIds));
        Map<Long, Set<String>> currentCategories = (products == null ? Collections.<DesignProductEntity>emptyList() : products)
                .stream()
                .filter(product -> product.getProductCategory() != null
                        && !product.getProductCategory().isBlank())
                .collect(Collectors.groupingBy(DesignProductEntity::getPackageId,
                        Collectors.mapping(DesignProductEntity::getProductCategory, Collectors.toSet())));
        List<DesignDrawingEntity> all = drawingService.list(new LambdaQueryWrapper<DesignDrawingEntity>()
                .in(DesignDrawingEntity::getPackageId, packageIds)
                .eq(DesignDrawingEntity::getIsDeleted, StatusConstants.NOT_DELETED)
                .orderByDesc(DesignDrawingEntity::getVersionSeq));
        Map<Long, Map<String, DesignDocVersionVO>> grouped = new java.util.LinkedHashMap<>();
        for (DesignDrawingEntity entity : all) {
            Set<String> categories = currentCategories.getOrDefault(entity.getPackageId(), Collections.emptySet());
            boolean currentCategoryDrawing = categories.isEmpty()
                    ? entity.getProductCategory() == null
                    : categories.contains(entity.getProductCategory());
            if (!currentCategoryDrawing) continue;
            grouped.computeIfAbsent(entity.getPackageId(), k -> new java.util.LinkedHashMap<>())
                    .putIfAbsent(entity.getProductCategory(), toDrawingVersionVO(entity));
        }
        Map<Long, List<DesignDocVersionVO>> result = new java.util.LinkedHashMap<>();
        grouped.forEach((pkg, values) -> result.put(pkg, new ArrayList<>(values.values())));
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
        boolean dataChanged = isPrintInfoChangedSince(packageId, null,
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
    private DesignDrawingEntity ensureDrawing(Long orderId, Long packageId, String productCategory) {
        String category = resolveCategory(packageId, productCategory);
        String lockKey = orderId + ":" + packageId + ":" + category;
        synchronized (DRAWING_LOCKS.computeIfAbsent(lockKey, ignored -> new Object())) {
            return ensureDrawingLocked(orderId, packageId, category);
        }
    }

    private DesignDrawingEntity ensureDrawingLocked(Long orderId, Long packageId, String productCategory) {
        log.info("按需确保图纸有效，orderId={}, packageId={}", orderId, packageId);

        // 加载基础数据
        OrderMainEntity order = orderMainService.getById(orderId);
        DesignPackageEntity pkg = lockPackageForDrawingMutation(orderId, packageId);

        // 前置校验：打印信息已填写
        LambdaQueryWrapper<DesignProductEntity> productQuery = new LambdaQueryWrapper<DesignProductEntity>()
                .eq(DesignProductEntity::getPackageId, packageId);
        if (productCategory != null) productQuery.eq(DesignProductEntity::getProductCategory, productCategory);
        long productCount = productService.count(productQuery);
        if (productCount == 0) {
            throw new BusinessException(ErrorCodeEnum.PRINT_INFO_REQUIRED);
        }

        // 查询最新版本
        DesignDrawingEntity latest = productCategory == null ? drawingService.getLatestVersion(packageId)
                : drawingService.getLatestVersion(packageId, productCategory);

        // 当前二维码只影响 AUTO 图纸；没有前端二维码时由生成过程使用后端兜底二维码。
        FileVO currentQrFile = currentQrFile(orderId);

        // 判断打印信息是否在上次生成后发生变化
        boolean dataChanged = isPrintInfoChangedSince(packageId, productCategory,
                latest != null ? latest.getGenerateTime() : null);

        if (latest == null) {
            // 场景1：首次，生成 A/1
            log.info("图纸首次生成，packageId={}", packageId);
            return doGenerateDrawing(order, pkg, productCategory, null, 1, currentQrFile);
        }

        boolean manual = StatusConstants.SOURCE_TYPE_MANUAL.equals(latest.getSourceType());
        boolean qrChanged = !manual
                && currentQrFile != null
                && !Objects.equals(currentQrFile.getId(), latest.getQrFileId());

        log.info("图纸生成决策，orderId={}, packageId={}, latestDrawingId={}, sourceType={}, "
                        + "currentQrFileId={}, latestQrFileId={}, dataChanged={}, qrChanged={}",
                orderId, packageId, latest.getId(), latest.getSourceType(),
                currentQrFile == null ? null : currentQrFile.getId(), latest.getQrFileId(),
                dataChanged, qrChanged);

        if (!dataChanged && !qrChanged) {
            // 场景4：打印信息未变，直接复用
            log.info("打印信息和二维码均未触发重新生成，复用已有图纸，packageId={}, version={}, qrFileId={}",
                    packageId, latest.getVersion(), latest.getQrFileId());
            return latest;
        }

        // 打印信息已变化
        if (!manual) {
            // 场景2：未封版（自动生成的版本），覆盖当前版本
            log.info("打印信息已变化，覆盖当前图纸版本，packageId={}, version={}", packageId, latest.getVersion());
            return doGenerateDrawing(order, pkg, productCategory, latest, latest.getVersionSeq(), currentQrFile);
        } else {
            // 场景3：已封版（手动上传的版本），新建下一版本
            log.info("打印信息已变化，新建下一图纸版本，packageId={}, prevVersion={}", packageId, latest.getVersion());
            return doGenerateDrawing(order, pkg, productCategory, null, latest.getVersionSeq() + 1, currentQrFile);
        }
    }

    private FileVO currentQrFile(Long orderId) {
        List<FileVO> files = fileService.listByBiz(FileBizTypeEnum.DRAWING_QR_IMAGE.getDictCode(), orderId);
        FileVO current = files == null || files.isEmpty() ? null : files.get(0);
        log.info("查询订单当前图纸二维码，orderId={}, bizType={}, fileCount={}, currentQrFileId={}",
                orderId, FileBizTypeEnum.DRAWING_QR_IMAGE.getDictCode(),
                files == null ? 0 : files.size(), current == null ? null : current.getId());
        return current;
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
    private boolean isPrintInfoChangedSince(Long packageId, String productCategory, LocalDateTime sinceTime) {
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

    private String resolveCategory(Long packageId, String requestedCategory) {
        List<DesignProductEntity> products = productService.list(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId));
        List<String> categories = (products == null ? Collections.<DesignProductEntity>emptyList() : products)
                .stream().map(DesignProductEntity::getProductCategory)
                .filter(category -> category != null && !category.isBlank()).distinct().toList();
        if (requestedCategory != null && !requestedCategory.isBlank()) {
            if (!categories.contains(requestedCategory)) {
                throw new BusinessException(ErrorCodeEnum.DOC_VERSION_NOT_FOUND);
            }
            return requestedCategory;
        }
        if (categories.isEmpty()) return null;
        if (categories.size() != 1) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "混合产品数据包必须指定 productCategory");
        }
        return categories.get(0);
    }

    private String buildDrawingFilename(OrderMainEntity order, String productCategory) {
        String categoryName = designQueryHelper.getDictName(productCategory);
        if (categoryName == null || categoryName.isBlank()) {
            categoryName = "未分类";
        }
        return order.getPatientName() + "-" + categoryName + "图纸.xlsx";
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

        // 上传 OSS（文件名前加患者姓名）
        String filename = order.getPatientName() + "指令单.xlsx";
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
            OrderMainEntity order, DesignPackageEntity pkg, String productCategory,
            DesignDrawingEntity toOverride, int versionSeq, FileVO qrFile) {

        Long packageId = pkg.getId();
        String version = "A/" + versionSeq;
        LocalDateTime now = LocalDateTime.now();

        // 查询产品列表和关联文件
        List<DesignProductEntity> products = productService.list(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId)
                        .orderByAsc(DesignProductEntity::getSortOrder));
        if (productCategory != null) {
            products = products.stream().filter(p -> Objects.equals(productCategory, p.getProductCategory())).toList();
        }
        List<Long> productIds = products.stream().map(DesignProductEntity::getId).toList();
        List<DesignProductFileEntity> allProductFiles = productFileService.listByProductIds(productIds);

        // 批量加载截图字节
        List<Long> packageFileIds = allProductFiles.stream()
                .map(DesignProductFileEntity::getPackageFileId).distinct().toList();
        Map<Long, String> screenshotFileIdMap = screenshotService.listFileIdsByPackageFileIds(packageFileIds);
        Map<Long, byte[]> screenshotBytesMap = loadScreenshotBytes(screenshotFileIdMap);

        List<DrawingExcelBuilder.ProductRow> drawingRows = expandDrawingRows(products, allProductFiles, screenshotBytesMap);

        // 生成图纸 Excel
        DrawingExcelBuilder.BuildContext ctx = buildDrawingContext(order, pkg, drawingRows, now, qrFile);
        log.info("图纸二维码快照准备完成，orderId={}, packageId={}, version={}, qrSource={}, qrFileId={}, qrBytes={}",
                order.getId(), packageId, version, ctx.getQrSource(), ctx.getQrFileId(),
                ctx.getQrBytes() == null ? 0 : ctx.getQrBytes().length);
        byte[] bytes;
        try {
            bytes = drawingBuilder.build(ctx);
        } catch (IOException e) {
            log.error("生成图纸 Excel 失败，packageId={}", packageId, e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }

        // 上传 OSS（文件名前加患者姓名）
        String filename = buildDrawingFilename(order, productCategory);
        FileVO fileVO = fileService.uploadBytes(bytes, filename, FileBizTypeEnum.DRAWING_FILE.getDictCode());

        // 持久化
        if (toOverride != null) {
            String oldTemplateFileId = toOverride.getTemplateFileId();
            try {
                // 覆盖现有版本，先记录旧文件ID以便删除
                toOverride.setTemplateFileId(fileVO.getId());
                toOverride.setTemplateFileUrl(fileVO.getFileUrl());
                toOverride.setProductCategory(productCategory);
                toOverride.setQrFileId(ctx.getQrFileId());
                toOverride.setGenerateTime(now);
                toOverride.setSourceType(StatusConstants.SOURCE_TYPE_AUTO);
                // 数据已变，重置确认状态，强制重新确认
                toOverride.setIsConfirmed(StatusConstants.NOT_CONFIRMED);
                toOverride.setConfirmTime(null);
                drawingService.updateById(toOverride);
            } catch (RuntimeException ex) {
                deleteGeneratedFile(fileVO.getId());
                throw ex;
            }
            // 删除旧模板文件，避免 OSS 泄漏；删除失败不影响已保存的新版本。
            deleteOldTemplateFile(oldTemplateFileId);
            log.info("图纸覆盖完成，packageId={}, version={}, qrSource={}, qrFileId={}",
                    packageId, version, ctx.getQrSource(), ctx.getQrFileId());
            return toOverride;
        } else {
            try {
                // 新建版本
                DesignDrawingEntity entity = new DesignDrawingEntity();
                entity.setOrderId(order.getId());
                entity.setPackageId(packageId);
                entity.setProductCategory(productCategory);
                entity.setVersion(version);
                entity.setVersionSeq(versionSeq);
                entity.setSourceType("AUTO");
                entity.setGenerateTime(now);
                entity.setTemplateFileId(fileVO.getId());
                entity.setTemplateFileUrl(fileVO.getFileUrl());
                entity.setQrFileId(ctx.getQrFileId());
                // 新版本初始为未确认状态
                entity.setIsConfirmed(StatusConstants.NOT_CONFIRMED);
                drawingService.save(entity);
                log.info("图纸新建完成，packageId={}, version={}, qrSource={}, qrFileId={}",
                        packageId, version, ctx.getQrSource(), ctx.getQrFileId());
                return entity;
            } catch (RuntimeException ex) {
                deleteGeneratedFile(fileVO.getId());
                throw ex;
            }
        }
    }

    private void deleteOldTemplateFile(String fileId) {
        if (fileId == null) {
            return;
        }
        try {
            fileService.deleteById(fileId);
        } catch (RuntimeException ex) {
            log.warn("旧图纸文件删除失败，保留文件供人工清理，fileId={}", fileId, ex);
        }
    }

    private void deleteGeneratedFile(String fileId) {
        if (fileId == null) {
            return;
        }
        try {
            fileService.deleteById(fileId);
        } catch (RuntimeException cleanupEx) {
            log.error("图纸元数据保存失败且新文件清理失败，fileId={}", fileId, cleanupEx);
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
                row.setDesignProductId(p.getId());
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
     * 锁定图纸所属数据包，保证同一数据包的生成、修订和确认按事务串行执行。
     */
    private DesignPackageEntity lockPackageForDrawingMutation(Long orderId, Long packageId) {
        DesignPackageEntity pkg = packageService.getOne(
                new LambdaQueryWrapper<DesignPackageEntity>()
                        .eq(DesignPackageEntity::getId, packageId)
                        .eq(DesignPackageEntity::getOrderId, orderId)
                        .last("FOR UPDATE"),
                false);
        if (pkg == null) {
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
            List<DrawingExcelBuilder.ProductRow> rows, LocalDateTime generateTime, FileVO qrFile) {
        DrawingExcelBuilder.BuildContext ctx = new DrawingExcelBuilder.BuildContext();
        ctx.setOrderCode(order.getOrderCode());
        ctx.setPackageCode(pkg.getPackageCode());
        ctx.setRemark(pkg.getRemark());
        ctx.setRows(rows);
        ctx.setDesignerName(order.getDesignerName());
        ctx.setGenerateDate(generateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        loadQrBytes(ctx, order.getId(), qrFile);
        return ctx;
    }

    /**
     * 优先读取前端上传的二维码；订单尚未上传时恢复后端二维码作为兜底。
     */
    private void loadQrBytes(DrawingExcelBuilder.BuildContext ctx, Long orderId, FileVO qrFile) {
        if (qrFile != null && qrFile.getId() != null) {
            try {
                byte[] bytes = fileService.downloadToBytes(qrFile.getId());
                if (bytes != null && bytes.length > 0) {
                    ctx.setQrBytes(bytes);
                    ctx.setQrFileId(qrFile.getId());
                    ctx.setQrSource(QR_SOURCE_FRONTEND_FILE);
                    log.info("图纸二维码读取成功，source={}, orderId={}, qrFileId={}, fileName={}, fileHash={}, bytes={}",
                            QR_SOURCE_FRONTEND_FILE, orderId, qrFile.getId(), qrFile.getFileName(),
                            qrFile.getFileHash(), bytes.length);
                    return;
                }
                log.warn("前端二维码文件为空，改用后端兜底二维码，orderId={}, qrFileId={}, fileName={}",
                        orderId, qrFile.getId(), qrFile.getFileName());
            } catch (IOException e) {
                log.warn("读取前端二维码失败，改用后端兜底二维码，orderId={}, qrFileId={}, fileName={}",
                        orderId, qrFile.getId(), qrFile.getFileName(), e);
            }
        } else {
            log.info("订单未找到前端二维码文件，使用后端兜底二维码，orderId={}, reason=FRONTEND_FILE_NOT_FOUND",
                    orderId);
        }

        try {
            byte[] fallbackBytes = generateFallbackQrCode(buildViewerQrContent(orderId), 500, 500);
            ctx.setQrBytes(fallbackBytes);
            ctx.setQrFileId(null);
            ctx.setQrSource(QR_SOURCE_BACKEND_FALLBACK);
            log.info("图纸二维码使用后端兜底生成，source={}, orderId={}, qrFileId=null, bytes={}, "
                            + "contentType=IMAGING_VIEWER_URL",
                    QR_SOURCE_BACKEND_FALLBACK, orderId, fallbackBytes.length);
        } catch (Exception e) {
            log.error("生成后端兜底二维码失败，orderId={}", orderId, e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }
    }

    private String buildViewerQrContent(Long orderId) {
        String baseUrl = configService.getConfigValue(SystemConfigKeyEnum.IMAGING_VIEWER_BASE_URL.getKey());
        if (baseUrl == null || baseUrl.isBlank()) {
            log.warn("影像查看器配置未设置，使用订单标识作为二维码兜底内容，orderId={}", orderId);
            return "order:" + orderId;
        }
        String json = String.format(
                "{\"paths\":{\"dcmPath\":{\"path\":\"/api/imaging/v1/dcm\",\"params\":{\"orderId\":%d},\"type\":\"post\"}," +
                "\"stlPath\":{\"path\":\"/api/imaging/v1/stl\",\"params\":{\"orderId\":%d},\"type\":\"post\"}," +
                "\"markPath\":{\"path\":\"/api/imaging/v1/mark\",\"params\":{},\"type\":\"post\"}}," +
                "\"token\":{\"Authorization\":\"\"}}",
                orderId, orderId);
        String kv = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        String content = baseUrl + "?kv=" + kv;
        log.info("后端二维码内容构建完成，orderId={}, viewerBaseUrlConfigured=true, payloadBytes={}, contentLength={}",
                orderId, json.getBytes(StandardCharsets.UTF_8).length, content.length());
        return content;
    }

    private byte[] generateFallbackQrCode(String content, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 0);

        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0x2563EB : 0xFFFFFF);
            }
        }

        try (InputStream logoStream = new ClassPathResource("static/ico.png").getInputStream()) {
            BufferedImage logo = ImageIO.read(logoStream);
            int logoSize = (int) (width * 0.25);
            int logoMargin = 2;
            int logoX = (width - logoSize) / 2;
            int logoY = (height - logoSize) / 2;
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(logoX - logoMargin, logoY - logoMargin,
                    logoSize + logoMargin * 2, logoSize + logoMargin * 2);
            graphics.drawImage(logo, logoX, logoY, logoSize, logoSize, null);
            graphics.dispose();
        } catch (Exception e) {
            log.warn("后端兜底二维码Logo嵌入失败，使用纯色二维码: {}", e.getMessage());
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        }
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
        vo.setProductCategory(entity.getProductCategory());
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
        vo.setProductCategory(entity.getProductCategory());
        vo.setConfirmTime(entity.getConfirmTime());
        return vo;
    }
}
