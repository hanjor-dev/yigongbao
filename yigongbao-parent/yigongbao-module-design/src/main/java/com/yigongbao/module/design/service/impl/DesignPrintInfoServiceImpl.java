package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.product.entity.ProductSpecEntity;
import com.yigongbao.module.basic.product.service.ProductService;
import com.yigongbao.module.basic.product.service.ProductSpecService;
import com.yigongbao.module.basic.product.vo.ProductVO;
import com.yigongbao.module.design.dto.SavePrintInfoDTO;
import com.yigongbao.module.design.dto.SavePrintInfoItemDTO;
import com.yigongbao.module.design.entity.DesignDrawingEntity;
import com.yigongbao.module.design.entity.DesignInstructionEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.entity.DesignProductFileEntity;
import com.yigongbao.module.design.mapper.DesignDrawingMapper;
import com.yigongbao.module.design.mapper.DesignInstructionMapper;
import com.yigongbao.module.design.service.DesignPackageFileService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.service.DesignPrintInfoService;
import com.yigongbao.module.design.service.DesignProductFileService;
import com.yigongbao.module.design.service.DesignProductService;
import com.yigongbao.module.design.vo.ColorGroupVO;
import com.yigongbao.module.design.vo.DesignProductVO;
import com.yigongbao.module.design.vo.DictOptionVO;
import com.yigongbao.module.design.vo.PrintInfoListVO;
import com.yigongbao.module.design.vo.PrintInfoOptionsVO;
import com.yigongbao.module.design.vo.PrintInfoProductVO;
import com.yigongbao.module.design.vo.PrintInfoSpecVO;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 打印信息管理 Service 实现类
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DesignPrintInfoServiceImpl implements DesignPrintInfoService {

    private final OrderMainService orderMainService;
    private final ProductService productService;
    private final ProductSpecService productSpecService;
    private final DesignPackageService packageService;
    private final DesignPackageFileService packageFileService;
    private final DesignProductService designProductService;
    private final DesignProductFileService productFileService;
    private final DictService dictService;
    private final DesignInstructionMapper instructionMapper;
    private final DesignDrawingMapper drawingMapper;

    /**
     * 获取打印信息选项数据以及包级已保存回显字段
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 选项 VO（designMode、产品树、材质、颜色分组、包级字段回显）
     */
    @Override
    public PrintInfoOptionsVO getOptions(Long orderId, Long packageId) {
        log.info("获取打印信息选项，orderId={}, packageId={}", orderId, packageId);

        // 1. 查订单
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        PrintInfoOptionsVO vo = new PrintInfoOptionsVO();
        vo.setDesignMode(null);

        // 2. 查所有 status=1 的产品（含 status=1 的规格），产品无规格时 specs 为空列表
        List<ProductVO> productVOs = productService.listAllWithSpecs();
        List<PrintInfoProductVO> products = productVOs.stream().map(p -> {
            PrintInfoProductVO productVO = new PrintInfoProductVO();
            productVO.setId(p.getId());
            productVO.setProductName(p.getProductName());
            productVO.setCategory(p.getCategory());
            productVO.setCategoryName(p.getCategoryName());
            List<PrintInfoSpecVO> specVOs = p.getSpecs() == null ? Collections.emptyList() :
                    p.getSpecs().stream().map(s -> {
                        PrintInfoSpecVO specVO = new PrintInfoSpecVO();
                        specVO.setId(s.getId());
                        specVO.setSpecName(s.getSpecName());
                        specVO.setCertId(s.getCertId());
                        specVO.setCertNo(s.getCertNo());
                        return specVO;
                    }).toList();
            productVO.setSpecs(specVOs);
            return productVO;
        }).toList();
        vo.setProducts(products);

        // 3. 查材质列表（dict typeCode=MATERIAL_TYPE），MATERIAL_TYPE_RESIN=树脂 标 isDefault=true
        List<DictVO> materialDicts = dictService.listByTypeCode(DictCodeConstants.MATERIAL_TYPE);
        List<DictOptionVO> materials = materialDicts.stream().map(d -> {
            DictOptionVO opt = new DictOptionVO();
            opt.setCode(d.getDictCode());
            opt.setName(d.getDictName());
            opt.setIsDefault(DictCodeConstants.MATERIAL_TYPE_RESIN.equals(d.getDictCode()));
            return opt;
        }).toList();
        vo.setMaterials(materials);

        // 4. 查颜色二级节点（dict typeCode=COLOR_TYPE）
        // 二级节点 dictValue 存产品大类 dict_code（如 17.1），供前端按产品分类过滤颜色
        List<DictVO> colorTree = dictService.listTreeByTypeCode(DictCodeConstants.COLOR_TYPE);
        List<ColorGroupVO> colorGroups = new ArrayList<>();
        if (CollUtil.isNotEmpty(colorTree)) {
            DictVO root = colorTree.get(0);
            if (root.getChildren() != null) {
                for (DictVO level2 : root.getChildren()) {
                    ColorGroupVO group = new ColorGroupVO();
                    // 二级节点 dictValue 存对应产品大类 dict_code（如 17.1）；null 表示通用
                    group.setCategoryCode(level2.getDictValue());
                    group.setCategoryName(level2.getDictName());
                    // 颜色本身直接使用二级节点（16.1=白色 等），无三级结构
                    DictOptionVO colorOpt = new DictOptionVO();
                    colorOpt.setCode(level2.getDictCode());
                    colorOpt.setName(level2.getDictName());
                    colorOpt.setIsDefault(false);
                    group.setColors(List.of(colorOpt));
                    colorGroups.add(group);
                }
            }
        }
        vo.setColorGroups(colorGroups);

        // 5. 回填包级已保存字段（productMark、packQuantity、remark）
        DesignPackageEntity pkg = packageService.getById(packageId);
        if (pkg != null && pkg.getOrderId().equals(orderId)) {
            vo.setProductMark(pkg.getProductMark());
            vo.setPackQuantity(pkg.getPackQuantity());
            vo.setRemark(pkg.getRemark());
        }

        log.info("获取打印信息选项成功，orderId={}, packageId={}", orderId, packageId);
        return vo;
    }

    /**
     * 查询数据包打印信息列表（按 sort_order 升序）
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 打印信息列表（包含数据包级别字段和产品列表）
     */
    @Override
    public PrintInfoListVO listPrintInfo(Long orderId, Long packageId) {
        log.info("查询打印信息列表，orderId={}, packageId={}", orderId, packageId);

        // 1. 校验 packageId 属于 orderId，并回填包级字段
        DesignPackageEntity pkg = packageService.getById(packageId);
        if (pkg == null || !pkg.getOrderId().equals(orderId)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND);
        }

        PrintInfoListVO result = new PrintInfoListVO();
        result.setProductMark(pkg.getProductMark());
        result.setPackQuantity(pkg.getPackQuantity());
        result.setRemark(pkg.getRemark());

        // 2. 查询产品列表
        List<DesignProductEntity> entities = designProductService.list(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId)
                        .orderByAsc(DesignProductEntity::getSortOrder));

        if (entities.isEmpty()) {
            result.setItems(Collections.emptyList());
            return result;
        }

        // 3. 批量加载 product 的 category/categoryName（按方案B：查询时关联获取）
        List<Long> productIds = entities.stream().map(DesignProductEntity::getProductId).distinct().toList();
        Map<Long, ProductVO> productMap = new java.util.HashMap<>();
        for (Long pid : productIds) {
            ProductVO product = productService.getById(pid);
            if (product != null) {
                productMap.put(pid, product);
            }
        }

        // 4. 批量查关联文件，按 designProductId 分组
        List<Long> designProductIds = entities.stream().map(DesignProductEntity::getId).toList();
        List<DesignProductFileEntity> allFiles = productFileService.listByProductIds(designProductIds);
        Map<Long, List<DesignProductFileEntity>> fileMap = allFiles.stream()
                .collect(Collectors.groupingBy(DesignProductFileEntity::getDesignProductId));

        // 5. 组装 VO
        List<DesignProductVO> items = entities.stream().map(e -> {
            DesignProductVO vo = toVO(e);
            // 补充 product 的 category/categoryName
            ProductVO product = productMap.get(e.getProductId());
            if (product != null) {
                vo.setCategory(product.getCategory());
                vo.setCategoryName(product.getCategoryName());
            }
            List<DesignProductFileEntity> files = fileMap.getOrDefault(e.getId(), List.of());
            vo.setFiles(files.stream().map(f -> {
                DesignProductVO.ProductFileVO fvo = new DesignProductVO.ProductFileVO();
                fvo.setId(f.getId());
                fvo.setPackageFileId(f.getPackageFileId());
                fvo.setPackageFileName(f.getPackageFileName());
                return fvo;
            }).toList());
            return vo;
        }).toList();

        result.setItems(items);
        log.info("查询打印信息列表成功，orderId={}, packageId={}, itemCount={}", orderId, packageId, items.size());
        return result;
    }

    /**
     * 保存打印信息（整包替换）
     * 空列表表示清空，合法操作
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param dto       请求 DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePrintInfo(Long orderId, Long packageId, SavePrintInfoDTO dto) {
        log.info("保存打印信息，orderId={}, packageId={}, itemCount={}",
                orderId, packageId, dto.getItems().size());

        // 1. 校验订单状态和操作人
        checkOrderAndPermission(orderId);

        // 2. 校验 packageId 属于 orderId
        DesignPackageEntity pkg = packageService.getById(packageId);
        if (pkg == null || !pkg.getOrderId().equals(orderId)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND);
        }

        List<SavePrintInfoItemDTO> items = dto.getItems();

        // 3. 删除旧产品行的关联文件（先删文件关联，再删产品行）
        List<Long> oldProductIds = designProductService.list(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId)
                        .select(DesignProductEntity::getId))
                .stream().map(DesignProductEntity::getId).toList();
        if (!oldProductIds.isEmpty()) {
            productFileService.removeByProductIds(oldProductIds);
        }

        // 4. 删除旧产品行
        designProductService.remove(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId));

        if (CollUtil.isNotEmpty(items)) {
            // 5. 校验所有 packageFileIds 均属于该 packageId
            Set<Long> allFileIds = items.stream()
                    .flatMap(item -> item.getPackageFileIds().stream())
                    .collect(Collectors.toSet());
            long validFileCount = packageFileService.count(
                    new LambdaQueryWrapper<DesignPackageFileEntity>()
                            .eq(DesignPackageFileEntity::getPackageId, packageId)
                            .in(DesignPackageFileEntity::getId, allFileIds));
            if (validFileCount != allFileIds.size()) {
                throw new BusinessException(ErrorCodeEnum.ORDER_FILE_NOT_FOUND, "部分文件不属于该数据包");
            }

            // 6. 校验 productId / specId，批量加载产品和 spec 对象（用于覆盖 productName/certNo）
            Set<Long> productIds = items.stream().map(SavePrintInfoItemDTO::getProductId).collect(Collectors.toSet());
            Set<Long> specIds = items.stream().map(SavePrintInfoItemDTO::getSpecId).collect(Collectors.toSet());
            List<ProductSpecEntity> specList = productSpecService.listByIds(specIds);
            Map<Long, ProductSpecEntity> specMap = specList.stream()
                    .collect(Collectors.toMap(ProductSpecEntity::getId, s -> s));

            // 批量加载产品信息，避免下方插入循环中 N+1 查询
            Map<Long, ProductVO> productMap = new java.util.HashMap<>();
            for (Long productId : productIds) {
                ProductVO product = productService.getById(productId);
                if (product == null || product.getStatus() == null || product.getStatus() != StatusConstants.NORMAL) {
                    throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
                }
                productMap.put(productId, product);
            }

            for (SavePrintInfoItemDTO item : items) {
                // 校验 specId 存在、status=1，且 spec.productId == productId
                ProductSpecEntity spec = specMap.get(item.getSpecId());
                if (spec == null || spec.getStatus() == null || spec.getStatus() != StatusConstants.NORMAL) {
                    throw new BusinessException(ErrorCodeEnum.PRODUCT_SPEC_NOT_FOUND);
                }
                if (!spec.getProductId().equals(item.getProductId())) {
                    throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
                }
            }

            // 7. 批量插入新产品行（sortOrder 由服务端按提交顺序赋值，忽略前端传值）
            List<DesignProductEntity> entities = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                SavePrintInfoItemDTO item = items.get(i);
                DesignProductEntity entity = new DesignProductEntity();
                BeanUtils.copyProperties(item, entity);
                entity.setOrderId(orderId);
                entity.setPackageId(packageId);
                // productName/certNo 均从主数据取，覆盖前端传值，保证 Excel 填充时不为空
                ProductVO product = productMap.get(item.getProductId());
                entity.setProductName(product != null ? product.getProductName() : null);
                ProductSpecEntity spec = specMap.get(item.getSpecId());
                entity.setCertNo(spec.getCertNo());
                entity.setSpecName(spec.getSpecName());
                // sortOrder 由服务端按提交顺序赋值（0-based），不信任前端传值
                entity.setSortOrder(i);
                entities.add(entity);
            }
            designProductService.saveBatch(entities);

            // 8. 批量插入关联文件行（遍历每个产品行及其 packageFileIds）
            // 预加载所有涉及的 packageFile 文件名，避免在循环中逐条查询
            Map<Long, String> fileNameMap = packageFileService.listByIds(allFileIds).stream()
                    .collect(Collectors.toMap(DesignPackageFileEntity::getId, DesignPackageFileEntity::getFileName));
            List<DesignProductFileEntity> fileEntities = new ArrayList<>();
            for (int i = 0; i < entities.size(); i++) {
                DesignProductEntity saved = entities.get(i);
                List<Long> fileIds = items.get(i).getPackageFileIds();
                for (int j = 0; j < fileIds.size(); j++) {
                    Long fileId = fileIds.get(j);
                    DesignProductFileEntity dpf = new DesignProductFileEntity();
                    dpf.setDesignProductId(saved.getId());
                    dpf.setPackageFileId(fileId);
                    dpf.setPackageFileName(fileNameMap.get(fileId));
                    dpf.setSortOrder(j);
                    fileEntities.add(dpf);
                }
            }
            productFileService.saveBatch(fileEntities);
        }

        // 9. 更新 design_package 包级字段（productMark、packQuantity、remark）
        DesignPackageEntity pkgUpdate = new DesignPackageEntity();
        pkgUpdate.setId(packageId);
        pkgUpdate.setProductMark(dto.getProductMark());
        pkgUpdate.setPackQuantity(dto.getPackQuantity());
        pkgUpdate.setRemark(dto.getRemark());
        packageService.updateById(pkgUpdate);

        // 10. 打印信息变化，重置该数据包的指令单和图纸确认状态（is_confirmed=0）
        resetConfirmedStatus(packageId);

        log.info("保存打印信息成功，orderId={}, packageId={}", orderId, packageId);
    }

    /**
     * 删除单条打印信息
     *
     * @param orderId     订单ID
     * @param packageId   数据包ID
     * @param printInfoId 打印信息ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePrintInfo(Long orderId, Long packageId, Long printInfoId) {
        log.info("删除打印信息，orderId={}, packageId={}, printInfoId={}", orderId, packageId, printInfoId);

        // 1. 校验订单状态和操作人
        checkOrderAndPermission(orderId);

        // 2. 查询并验证 orderId 和 packageId 匹配
        DesignProductEntity entity = designProductService.getById(printInfoId);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        if (!entity.getOrderId().equals(orderId) || !entity.getPackageId().equals(packageId)) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 3. 先删关联文件行，再删产品行
        productFileService.removeByProductId(printInfoId);
        designProductService.removeById(printInfoId);

        // 4. 打印信息变化，重置该数据包的指令单和图纸确认状态（is_confirmed=0）
        resetConfirmedStatus(packageId);

        log.info("删除打印信息成功，printInfoId={}", printInfoId);
    }

    // ==================== 私有方法 ====================

    /**
     * 重置数据包下所有指令单和图纸的确认状态（打印信息变化时调用）
     *
     * @param packageId 数据包ID
     */
    private void resetConfirmedStatus(Long packageId) {
        instructionMapper.update(null,
                new LambdaUpdateWrapper<DesignInstructionEntity>()
                        .eq(DesignInstructionEntity::getPackageId, packageId)
                        .set(DesignInstructionEntity::getIsConfirmed, StatusConstants.NOT_CONFIRMED)
                        .set(DesignInstructionEntity::getConfirmTime, null));
        drawingMapper.update(null,
                new LambdaUpdateWrapper<DesignDrawingEntity>()
                        .eq(DesignDrawingEntity::getPackageId, packageId)
                        .set(DesignDrawingEntity::getIsConfirmed, StatusConstants.NOT_CONFIRMED)
                        .set(DesignDrawingEntity::getConfirmTime, null));
        log.info("重置数据包确认状态成功，packageId={}", packageId);
    }

    /**
     * 校验订单状态和操作权限（当前登录用户必须是该订单的设计师）
     *
     * @param orderId 订单ID
     * @return 订单实体
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
     * 打印信息实体转 VO
     */
    private DesignProductVO toVO(DesignProductEntity entity) {
        DesignProductVO vo = new DesignProductVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
