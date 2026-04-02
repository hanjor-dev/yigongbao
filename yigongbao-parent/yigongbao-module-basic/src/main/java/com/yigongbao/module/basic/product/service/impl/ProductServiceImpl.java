package com.yigongbao.module.basic.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.product.convert.ProductConvert;
import com.yigongbao.module.basic.product.dto.CreateProductDTO;
import com.yigongbao.module.basic.product.dto.ProductCategoryDTO;
import com.yigongbao.module.basic.product.dto.ProductListDTO;
import com.yigongbao.module.basic.product.dto.ProductPageDTO;
import com.yigongbao.module.basic.product.dto.UpdateProductDTO;
import com.yigongbao.module.basic.product.entity.ProductEntity;
import com.yigongbao.module.basic.product.mapper.ProductMapper;
import com.yigongbao.module.basic.product.service.ProductService;
import com.yigongbao.module.basic.product.vo.ProductVO;
import com.yigongbao.module.basic.registrationCert.service.RegistrationCertService;
import com.yigongbao.module.basic.registrationCert.vo.RegistrationCertVO;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 产品型号 Service 实现类
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl extends ServiceImpl<ProductMapper, ProductEntity> implements ProductService {

    private final RegistrationCertService registrationCertService;
    private final CodeGeneratorService codeGeneratorService;

    /**
     * 分页查询产品列表
     */
    @Override
    public IPage<ProductVO> listProducts(ProductPageDTO dto) {
        log.info("分页查询产品列表，dto={}", dto);
        try {
            int pageNum = dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum();
            int pageSize = dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : dto.getPageSize();
            Page<ProductEntity> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<ProductEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StringUtils.hasText(dto.getProductName()), ProductEntity::getProductName, dto.getProductName())
                    .eq(StringUtils.hasText(dto.getCategory()), ProductEntity::getCategory, dto.getCategory())
                    .eq(Objects.nonNull(dto.getCertId()), ProductEntity::getCertId, dto.getCertId())
                    .eq(Objects.nonNull(dto.getStatus()), ProductEntity::getStatus, dto.getStatus())
                    .orderByDesc(ProductEntity::getCreateTime);

            IPage<ProductEntity> pageResult = baseMapper.selectPage(page, wrapper);

            List<ProductVO> voList = pageResult.getRecords().stream()
                    .map(ProductConvert::toVO)
                    .toList();
            fillExtraFieldsBatch(voList);

            IPage<ProductVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
            voPage.setRecords(voList);

            log.info("分页查询产品列表成功，总数={}", pageResult.getTotal());
            return voPage;
        } catch (Exception e) {
            log.error("分页查询产品列表异常", e);
            throw e;
        }
    }

    /**
     * 查询所有产品列表
     */
    @Override
    public List<ProductVO> listAll(ProductListDTO dto) {
        log.info("查询所有产品列表，dto={}", dto);
        try {
            LambdaQueryWrapper<ProductEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StringUtils.hasText(dto.getProductName()), ProductEntity::getProductName, dto.getProductName())
                    .eq(StringUtils.hasText(dto.getCategory()), ProductEntity::getCategory, dto.getCategory())
                    .eq(Objects.nonNull(dto.getStatus()), ProductEntity::getStatus, dto.getStatus())
                    .orderByDesc(ProductEntity::getCreateTime);

            List<ProductEntity> list = list(wrapper);
            List<ProductVO> voList = list.stream().map(ProductConvert::toVO).toList();
            fillExtraFieldsBatch(voList);

            log.info("查询所有产品列表成功，数量={}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("查询所有产品列表异常", e);
            throw e;
        }
    }

    /**
     * 根据ID查询产品
     */
    @Override
    public ProductVO getById(Long id) {
        log.info("根据ID查询产品，id={}", id);
        try {
            ProductEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("产品不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
            }
            ProductVO vo = ProductConvert.toVO(entity);
            fillExtraFields(vo, entity);
            log.info("查询产品成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询产品异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 创建产品
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CreateProductDTO dto) {
        log.info("创建产品，productName={}", dto.getProductName());
        try {
            // 校验注册证是否存在，不存在则抛异常
            if (dto.getCertId() != null) {
                if (registrationCertService.getById(dto.getCertId()) == null) {
                    log.warn("注册证不存在，certId={}", dto.getCertId());
                    throw new BusinessException(ErrorCodeEnum.CERT_NOT_FOUND);
                }
            }

            ProductEntity entity = ProductConvert.toEntity(dto);
            // 自动生成产品编码
            String productCode = codeGeneratorService.generate(CodeRuleConstants.PRODUCT_CODE);
            entity.setProductCode(productCode);
            // 校验编码唯一性
            if (super.count(new LambdaQueryWrapper<ProductEntity>()
                    .eq(ProductEntity::getProductCode, productCode)) > 0) {
                log.warn("产品编码已存在，productCode={}", productCode);
                throw new BusinessException(ErrorCodeEnum.PRODUCT_EXISTS);
            }
            if (entity.getStatus() == null) {
                entity.setStatus(StatusConstants.NORMAL);
            }

            save(entity);
            log.info("创建产品成功，id={}, productName={}", entity.getId(), dto.getProductName());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建产品异常，productName={}", dto.getProductName(), e);
            throw e;
        }
    }

    /**
     * 更新产品
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UpdateProductDTO dto) {
        log.info("更新产品，id={}", id);
        try {
            ProductEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("产品不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
            }

            // 校验注册证是否存在
            if (dto.getCertId() != null && !dto.getCertId().equals(entity.getCertId())) {
                registrationCertService.getById(dto.getCertId());
            }

            BeanUtils.copyProperties(dto, entity, "id", "productCode", "createTime", "updateTime", "createBy", "updateBy");
            updateById(entity);
            log.info("更新产品成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新产品异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 删除产品
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        log.info("删除产品，id={}", id);
        try {
            ProductEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("产品不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
            }
            removeById(id);
            log.info("删除产品成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除产品异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 按注册证查询产品
     */
    @Override
    public List<ProductVO> listByCertId(Long certId) {
        log.info("按注册证查询产品，certId={}", certId);
        try {
            LambdaQueryWrapper<ProductEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProductEntity::getCertId, certId)
                    .eq(ProductEntity::getStatus, StatusConstants.NORMAL)
                    .orderByDesc(ProductEntity::getCreateTime);

            List<ProductEntity> list = list(wrapper);
            List<ProductVO> voList = list.stream().map(ProductConvert::toVO).toList();
            fillExtraFieldsBatch(voList);

            log.info("按注册证查询产品成功，数量={}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("按注册证查询产品异常，certId={}", certId, e);
            throw e;
        }
    }

    /**
     * 按分类查询产品
     */
    @Override
    public List<ProductVO> listByCategory(String category) {
        log.info("按分类查询产品，category={}", category);
        try {
            LambdaQueryWrapper<ProductEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProductEntity::getCategory, category)
                    .eq(ProductEntity::getStatus, StatusConstants.NORMAL)
                    .orderByDesc(ProductEntity::getCreateTime);

            List<ProductEntity> list = list(wrapper);
            List<ProductVO> voList = list.stream().map(ProductConvert::toVO).toList();
            fillExtraFieldsBatch(voList);

            log.info("按分类查询产品成功，数量={}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("按分类查询产品异常，category={}", category, e);
            throw e;
        }
    }

    /**
     * 填充额外字段（单条，用于 getById）
     */
    private void fillExtraFields(ProductVO vo, ProductEntity entity) {
        if (entity.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(entity.getStatus()));
        }
        if (entity.getCertId() != null) {
            try {
                RegistrationCertVO cert = registrationCertService.getById(entity.getCertId());
                if (cert != null) {
                    vo.setCertCode(cert.getCertCode());
                }
            } catch (Exception e) {
                log.debug("获取注册证信息失败，certId={}", entity.getCertId());
            }
        }
    }

    /**
     * 批量填充额外字段（消除 N+1 查询）
     *
     * @param voList 视图对象列表
     */
    private void fillExtraFieldsBatch(List<ProductVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        // 批量收集所有 certId
        Set<Long> certIds = voList.stream()
                .map(ProductVO::getCertId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 一次查询所有注册证，以 Map 缓存
        Map<Long, RegistrationCertVO> certMap = new HashMap<>();
        if (!certIds.isEmpty()) {
            List<RegistrationCertVO> certList = registrationCertService.listVOByIds(new ArrayList<>(certIds));
            certList.forEach(cert -> certMap.put(cert.getId(), cert));
        }

        // 填充 certCode（statusName 在 Controller 统一处理）
        for (ProductVO vo : voList) {
            if (vo.getCertId() != null && certMap.containsKey(vo.getCertId())) {
                vo.setCertCode(certMap.get(vo.getCertId()).getCertCode());
            }
        }
    }
}
