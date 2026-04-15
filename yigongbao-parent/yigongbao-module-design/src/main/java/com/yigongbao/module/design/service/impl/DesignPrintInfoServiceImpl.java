package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.yigongbao.module.basic.product.vo.ProductSpecVO;
import com.yigongbao.module.design.dto.SavePrintInfoDTO;
import com.yigongbao.module.design.dto.SavePrintInfoItemDTO;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.service.DesignPackageFileService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.service.DesignPrintInfoService;
import com.yigongbao.module.design.service.DesignProductService;
import com.yigongbao.module.design.vo.ColorGroupVO;
import com.yigongbao.module.design.vo.DesignProductVO;
import com.yigongbao.module.design.vo.DictOptionVO;
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
    private final DictService dictService;

    /**
     * 获取打印信息选项数据
     * 不校验操作人，任何登录用户均可查询选项
     *
     * @param orderId 订单ID
     * @return 选项 VO（designMode、产品树、材质、颜色分组）
     */
    @Override
    public PrintInfoOptionsVO getOptions(Long orderId) {
        log.info("获取打印信息选项，orderId={}", orderId);

        // 1. 查订单，取 designMode（暂无该字段，预留为 null）
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        PrintInfoOptionsVO vo = new PrintInfoOptionsVO();
        // designMode 暂未存于 order_main，返回 null
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

        // 3. 查材质列表（dict typeCode="15"），15.1=树脂 标 isDefault=true
        List<DictVO> materialDicts = dictService.listByTypeCode("15");
        List<DictOptionVO> materials = materialDicts.stream().map(d -> {
            DictOptionVO opt = new DictOptionVO();
            opt.setCode(d.getDictCode());
            opt.setName(d.getDictName());
            opt.setIsDefault("15.1".equals(d.getDictCode()));
            return opt;
        }).toList();
        vo.setMaterials(materials);

        // 4. 查颜色三级树（dict typeCode="16"）
        // 结构：16（根）→ 16.x（二级，dictValue 存产品大类 dict_code 如 17.1）→ 16.x.y（三级，颜色选项）
        List<DictVO> colorTree = dictService.listTreeByTypeCode("16");
        List<ColorGroupVO> colorGroups = new ArrayList<>();
        if (CollUtil.isNotEmpty(colorTree)) {
            // listTreeByTypeCode 返回 [根节点]，根节点的 children 是二级节点
            DictVO root = colorTree.get(0);
            if (root.getChildren() != null) {
                for (DictVO level2 : root.getChildren()) {
                    ColorGroupVO group = new ColorGroupVO();
                    // 二级节点 dictValue 存对应产品大类 dict_code（如 17.1）
                    group.setCategoryCode(level2.getDictValue());
                    group.setCategoryName(level2.getDictName());
                    List<DictOptionVO> colors = new ArrayList<>();
                    if (level2.getChildren() != null) {
                        for (DictVO level3 : level2.getChildren()) {
                            DictOptionVO colorOpt = new DictOptionVO();
                            colorOpt.setCode(level3.getDictCode());
                            colorOpt.setName(level3.getDictName());
                            colorOpt.setIsDefault(false);
                            colors.add(colorOpt);
                        }
                    }
                    group.setColors(colors);
                    colorGroups.add(group);
                }
            }
        }
        vo.setColorGroups(colorGroups);

        log.info("获取打印信息选项成功，orderId={}", orderId);
        return vo;
    }

    /**
     * 查询数据包打印信息列表（按 sort_order 升序）
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 打印信息列表
     */
    @Override
    public List<DesignProductVO> listPrintInfo(Long orderId, Long packageId) {
        log.info("查询打印信息列表，orderId={}, packageId={}", orderId, packageId);

        // 校验 packageId 属于 orderId
        DesignPackageEntity pkg = packageService.getById(packageId);
        if (pkg == null || !pkg.getOrderId().equals(orderId)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND);
        }

        List<DesignProductEntity> list = designProductService.list(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId)
                        .orderByAsc(DesignProductEntity::getSortOrder));

        return list.stream().map(this::toVO).toList();
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

        // 2. 校验 packageId 的 orderId 等于传入 orderId
        DesignPackageEntity pkg = packageService.getById(packageId);
        if (pkg == null || !pkg.getOrderId().equals(orderId)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND);
        }

        List<SavePrintInfoItemDTO> items = dto.getItems();

        if (CollUtil.isNotEmpty(items)) {
            // 3. 校验每条 packageFileId 属于该 packageId
            Set<Long> fileIds = items.stream().map(SavePrintInfoItemDTO::getPackageFileId)
                    .collect(Collectors.toSet());
            long validFileCount = packageFileService.count(
                    new LambdaQueryWrapper<DesignPackageFileEntity>()
                            .eq(DesignPackageFileEntity::getPackageId, packageId)
                            .in(DesignPackageFileEntity::getId, fileIds));
            if (validFileCount != fileIds.size()) {
                throw new BusinessException(ErrorCodeEnum.ORDER_FILE_NOT_FOUND, "部分文件不属于该数据包");
            }

            // 4. 校验每条 productId / specId，并批量加载 spec 对象（用于覆盖 certNo）
            Set<Long> specIds = items.stream().map(SavePrintInfoItemDTO::getSpecId).collect(Collectors.toSet());
            List<ProductSpecEntity> specList = productSpecService.listByIds(specIds);
            Map<Long, ProductSpecEntity> specMap = specList.stream()
                    .collect(Collectors.toMap(ProductSpecEntity::getId, s -> s));

            for (SavePrintInfoItemDTO item : items) {
                // 校验 productId 存在且 status=1
                ProductVO product = productService.getById(item.getProductId());
                if (product == null || !Integer.valueOf(StatusConstants.NORMAL).equals(product.getStatus())) {
                    throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
                }

                // 校验 specId 存在、status=1，且 spec.productId == productId
                ProductSpecEntity spec = specMap.get(item.getSpecId());
                if (spec == null || !Integer.valueOf(StatusConstants.NORMAL).equals(spec.getStatus())) {
                    throw new BusinessException(ErrorCodeEnum.PRODUCT_SPEC_NOT_FOUND);
                }
                if (!spec.getProductId().equals(item.getProductId())) {
                    throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
                }
            }

            // 5. 删除旧记录
            designProductService.remove(
                    new LambdaQueryWrapper<DesignProductEntity>()
                            .eq(DesignProductEntity::getPackageId, packageId));

            // 6. 批量插入新记录，certNo 从 spec 对象中取
            List<DesignProductEntity> entities = new ArrayList<>();
            for (SavePrintInfoItemDTO item : items) {
                DesignProductEntity entity = new DesignProductEntity();
                BeanUtils.copyProperties(item, entity);
                entity.setOrderId(orderId);
                entity.setPackageId(packageId);
                // certNo 从 spec 对象中取，覆盖前端传值
                ProductSpecEntity spec = specMap.get(item.getSpecId());
                entity.setCertNo(spec.getCertNo());
                entities.add(entity);
            }
            designProductService.saveBatch(entities);
        } else {
            // 空列表：清空该数据包的所有打印信息
            designProductService.remove(
                    new LambdaQueryWrapper<DesignProductEntity>()
                            .eq(DesignProductEntity::getPackageId, packageId));
        }

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

        // 3. 逻辑删除
        designProductService.removeById(printInfoId);
        log.info("删除打印信息成功，printInfoId={}", printInfoId);
    }

    // ==================== 私有方法 ====================

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
     * 打印信息实体转 VO
     */
    private DesignProductVO toVO(DesignProductEntity entity) {
        DesignProductVO vo = new DesignProductVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
