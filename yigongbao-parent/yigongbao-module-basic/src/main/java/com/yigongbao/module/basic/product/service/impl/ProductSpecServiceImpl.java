package com.yigongbao.module.basic.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.service.SpecReferenceChecker;
import com.yigongbao.module.basic.product.dto.CreateProductSpecDTO;
import com.yigongbao.module.basic.product.dto.UpdateProductSpecDTO;
import com.yigongbao.module.basic.product.entity.ProductEntity;
import com.yigongbao.module.basic.product.entity.ProductSpecEntity;
import com.yigongbao.module.basic.product.mapper.ProductMapper;
import com.yigongbao.module.basic.product.mapper.ProductSpecMapper;
import com.yigongbao.module.basic.product.service.ProductSpecService;
import com.yigongbao.module.basic.product.vo.ProductSpecVO;
import com.yigongbao.module.basic.registrationCert.service.RegistrationCertService;
import com.yigongbao.module.basic.registrationCert.vo.RegistrationCertVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 产品规格 Service 实现类
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSpecServiceImpl extends ServiceImpl<ProductSpecMapper, ProductSpecEntity>
        implements ProductSpecService {

    private final ProductMapper productMapper;

    @Autowired(required = false)
    private RegistrationCertService registrationCertService;

    /**
     * 规格引用检查器，由 design 模块实现并注入（Optional，允许不存在）
     */
    @Autowired(required = false)
    private SpecReferenceChecker specReferenceChecker;

    /**
     * 查询指定产品下的所有规格列表（按 sort 升序）
     *
     * @param productId 产品ID
     * @return 规格列表
     */
    @Override
    public List<ProductSpecVO> listByProductId(Long productId) {
        List<ProductSpecEntity> list = list(new LambdaQueryWrapper<ProductSpecEntity>()
                .eq(ProductSpecEntity::getProductId, productId)
                .orderByAsc(ProductSpecEntity::getSort)
                .orderByAsc(ProductSpecEntity::getId));
        return list.stream().map(this::toVO).toList();
    }

    /**
     * 创建规格
     * 1. 校验产品存在
     * 2. 校验同产品下规格名称唯一
     * 3. 插入规格
     *
     * @param productId 产品ID
     * @param dto       创建 DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(Long productId, CreateProductSpecDTO dto) {
        // 1. 校验产品存在
        ProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            log.warn("产品不存在: productId={}", productId);
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }

        // 2. 校验同产品下规格名称唯一
        long count = count(new LambdaQueryWrapper<ProductSpecEntity>()
                .eq(ProductSpecEntity::getProductId, productId)
                .eq(ProductSpecEntity::getSpecName, dto.getSpecName()));
        if (count > 0) {
            log.warn("同产品下规格名称已存在: productId={}, specName={}", productId, dto.getSpecName());
            throw new BusinessException(ErrorCodeEnum.PRODUCT_SPEC_EXISTS);
        }

        // 3. 构建并插入规格
        ProductSpecEntity entity = new ProductSpecEntity();
        BeanUtils.copyProperties(dto, entity);
        entity.setProductId(productId);
        // 根据 certId 查询注册证号并写入冗余字段
        if (dto.getCertId() != null && registrationCertService != null) {
            RegistrationCertVO cert = registrationCertService.getById(dto.getCertId());
            entity.setCertNo(cert != null ? cert.getCertCode() : null);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(StatusConstants.NORMAL);
        }
        if (entity.getSort() == null) {
            entity.setSort(0);
        }

        save(entity);
        log.info("创建产品规格: specId={}, productId={}, specName={}", entity.getId(), productId, dto.getSpecName());
    }

    /**
     * 更新规格
     * 1. 校验规格存在
     * 2. 若修改了规格名称，校验同产品下名称唯一
     * 3. 更新
     *
     * @param specId 规格ID
     * @param dto    更新 DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long specId, UpdateProductSpecDTO dto) {
        // 1. 校验规格存在
        ProductSpecEntity entity = getById(specId);
        if (entity == null) {
            log.warn("产品规格不存在: specId={}", specId);
            throw new BusinessException(ErrorCodeEnum.PRODUCT_SPEC_NOT_FOUND);
        }

        // 2. 若修改了规格名称，校验同产品下名称唯一
        if (dto.getSpecName() != null && !dto.getSpecName().equals(entity.getSpecName())) {
            long count = count(new LambdaQueryWrapper<ProductSpecEntity>()
                    .eq(ProductSpecEntity::getProductId, entity.getProductId())
                    .eq(ProductSpecEntity::getSpecName, dto.getSpecName()));
            if (count > 0) {
                log.warn("同产品下规格名称已存在: productId={}, specName={}", entity.getProductId(), dto.getSpecName());
                throw new BusinessException(ErrorCodeEnum.PRODUCT_SPEC_EXISTS);
            }
        }

        // 3. 更新
        Long oldCertId = entity.getCertId();
        BeanUtils.copyProperties(dto, entity, "id", "productId", "createTime", "updateTime", "createBy", "updateBy", "certNo");
        // 若 certId 有变化或 certNo 为空，则重新查询 certNo；否则保留原值
        if (dto.getCertId() != null) {
            if (!dto.getCertId().equals(oldCertId) || entity.getCertNo() == null) {
                RegistrationCertVO cert = registrationCertService != null
                        ? registrationCertService.getById(dto.getCertId()) : null;
                entity.setCertNo(cert != null ? cert.getCertCode() : null);
            }
        } else if (dto.getCertId() == null && oldCertId != null) {
            // certId 被清空，同步清空 certNo
            entity.setCertNo(null);
        }
        updateById(entity);
        log.info("更新产品规格: specId={}", specId);
    }

    /**
     * 删除规格（逻辑删除）
     * 1. 校验规格存在
     * 2. 校验规格未被 design_product 引用（is_deleted=0 的记录不计已软删除）
     * 3. 逻辑删除
     *
     * @param specId 规格ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long specId) {
        // 1. 校验规格存在
        ProductSpecEntity entity = getById(specId);
        if (entity == null) {
            log.warn("产品规格不存在: specId={}", specId);
            throw new BusinessException(ErrorCodeEnum.PRODUCT_SPEC_NOT_FOUND);
        }

        // 2. 校验规格未被 design_product 引用
        if (specReferenceChecker != null && specReferenceChecker.isSpecInUse(specId)) {
            log.warn("规格已被打印信息引用，无法删除: specId={}", specId);
            throw new BusinessException(ErrorCodeEnum.PRODUCT_SPEC_IN_USE);
        }

        // 3. 逻辑删除
        removeById(specId);
        log.info("删除产品规格: specId={}", specId);
    }

    /**
     * 查询产品下是否存在未删除的规格
     *
     * @param productId 产品ID
     * @return 是否存在规格
     */
    @Override
    public boolean existsByProductId(Long productId) {
        return count(new LambdaQueryWrapper<ProductSpecEntity>()
                .eq(ProductSpecEntity::getProductId, productId)) > 0;
    }

    /**
     * 规格实体转 VO
     */
    private ProductSpecVO toVO(ProductSpecEntity entity) {
        ProductSpecVO vo = new ProductSpecVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setStatusName(StatusConstants.getStatusName(entity.getStatus()));
        return vo;
    }
}
