package com.yigongbao.module.basic.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.product.convert.ProductConvert;
import com.yigongbao.module.basic.product.dto.CreateProductDTO;
import com.yigongbao.module.basic.product.dto.ProductListDTO;
import com.yigongbao.module.basic.product.dto.ProductPageDTO;
import com.yigongbao.module.basic.product.dto.UpdateProductDTO;
import com.yigongbao.module.basic.product.entity.ProductEntity;
import com.yigongbao.module.basic.product.entity.ProductSpecEntity;
import com.yigongbao.module.basic.product.mapper.ProductMapper;
import com.yigongbao.module.basic.product.service.ProductService;
import com.yigongbao.module.basic.product.service.ProductSpecService;
import com.yigongbao.module.basic.product.vo.ProductSpecVO;
import com.yigongbao.module.basic.product.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import cn.hutool.core.util.StrUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 产品 Service 实现类
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl extends ServiceImpl<ProductMapper, ProductEntity> implements ProductService {

    @Lazy
    private final ProductSpecService productSpecService;

    /**
     * 分页查询产品列表
     *
     * @param pageNum   页码
     * @param pageSize  每页条数
     * @param productName 产品名称（模糊）
     * @param category  大类 dict_code
     * @param certId    注册证ID（已废弃，保留参数兼容）
     * @param status    状态
     * @return 分页结果
     */
    @Override
    public IPage<ProductVO> listProducts(int pageNum, int pageSize, String productName,
                                         String category, Long certId, Integer status) {
        log.info("分页查询产品列表，pageNum={}, pageSize={}, productName={}, category={}, status={}",
                pageNum, pageSize, productName, category, status);
        try {
            Page<ProductEntity> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<ProductEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StringUtils.hasText(productName), ProductEntity::getProductName, productName)
                    .eq(StringUtils.hasText(category), ProductEntity::getCategory, category)
                    .eq(Objects.nonNull(status), ProductEntity::getStatus, status)
                    .orderByDesc(ProductEntity::getCreateTime);

            IPage<ProductEntity> pageResult = baseMapper.selectPage(page, wrapper);

            List<ProductVO> voList = pageResult.getRecords().stream()
                    .map(ProductConvert::toVO)
                    .toList();
            fillStatusName(voList);

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
     *
     * @param productName 产品名称（模糊，可空）
     * @param category    大类 dict_code（可空）
     * @param status      状态（可空）
     * @return 产品列表
     */
    @Override
    public List<ProductVO> listAll(String productName, String category, Integer status) {
        log.info("查询所有产品列表，productName={}, category={}, status={}", productName, category, status);
        try {
            LambdaQueryWrapper<ProductEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StringUtils.hasText(productName), ProductEntity::getProductName, productName)
                    .eq(StringUtils.hasText(category), ProductEntity::getCategory, category)
                    .eq(Objects.nonNull(status), ProductEntity::getStatus, status)
                    .orderByDesc(ProductEntity::getCreateTime);

            List<ProductEntity> list = list(wrapper);
            List<ProductVO> voList = list.stream().map(ProductConvert::toVO).toList();
            fillStatusName(voList);

            log.info("查询所有产品列表成功，数量={}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("查询所有产品列表异常", e);
            throw e;
        }
    }

    /**
     * 根据ID查询产品（含规格列表）
     *
     * @param id 产品ID
     * @return 产品VO（含 specs）
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
            vo.setStatusName(StatusConstants.getStatusName(entity.getStatus()));
            // 填充规格列表
            vo.setSpecs(productSpecService.listByProductId(id));
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
     *
     * @param dto 创建 DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CreateProductDTO dto) {
        log.info("创建产品，productName={}, category={}, categoryName={}",
                dto.getProductName(), dto.getCategory(), dto.getCategoryName());
        try {
            // 校验产品名称唯一性
            long nameCount = super.count(new LambdaQueryWrapper<ProductEntity>()
                    .eq(ProductEntity::getProductName, dto.getProductName()));
            if (nameCount > 0) {
                log.warn("产品名称已存在，productName={}", dto.getProductName());
                throw new BusinessException(ErrorCodeEnum.PRODUCT_EXISTS);
            }

            // 校验 categoryName 必须提供（架构约束：basic 模块无法依赖 system 模块的 DictService）
            if (StrUtil.isNotBlank(dto.getCategory()) && StrUtil.isBlank(dto.getCategoryName())) {
                log.warn("产品类型名称未提供，category={}", dto.getCategory());
                throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "产品类型名称");
            }

            ProductEntity entity = ProductConvert.toEntity(dto);
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
     *
     * @param id  产品ID
     * @param dto 更新 DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UpdateProductDTO dto) {
        log.info("更新产品，id={}, category={}, categoryName={}", id, dto.getCategory(), dto.getCategoryName());
        try {
            ProductEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("产品不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
            }

            // 校验：若更新 category，则 categoryName 也必须提供（架构约束：basic 模块无法依赖 system 模块的 DictService）
            if (StrUtil.isNotBlank(dto.getCategory()) && StrUtil.isBlank(dto.getCategoryName())) {
                log.warn("产品类型变更时未提供类型名称，category={}", dto.getCategory());
                throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "产品类型名称");
            }

            BeanUtils.copyProperties(dto, entity, "id", "createTime", "updateTime", "createBy", "updateBy", "categoryName");
            // categoryName 是冗余字段，不允许被 null 覆盖；若 DTO 中有值则更新
            if (StrUtil.isNotBlank(dto.getCategoryName())) {
                entity.setCategoryName(dto.getCategoryName());
            }
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
     * 删除产品（逻辑删除）
     * 有规格时拒绝删除，抛出 PRODUCT_HAS_SPECS
     *
     * @param id 产品ID
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
            // 校验产品下是否存在规格
            if (productSpecService.existsByProductId(id)) {
                log.warn("产品下存在规格，无法删除，id={}", id);
                throw new BusinessException(ErrorCodeEnum.PRODUCT_HAS_SPECS);
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
     * 按注册证查询产品（该字段已迁移至规格层，此方法保留空实现兼容旧接口）
     *
     * @param certId 注册证ID
     * @return 产品列表（始终为空，注册证关联已移至规格层）
     */
    @Override
    public List<ProductVO> listByCertId(Long certId) {
        log.info("按注册证查询产品（已迁移至规格层），certId={}", certId);
        return List.of();
    }

    /**
     * 按分类查询产品
     *
     * @param category 大类 dict_code
     * @return 产品列表
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
            fillStatusName(voList);

            log.info("按分类查询产品成功，数量={}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("按分类查询产品异常，category={}", category, e);
            throw e;
        }
    }

    /**
     * 批量填充状态名称
     *
     * @param voList 产品 VO 列表
     */
    private void fillStatusName(List<ProductVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        for (ProductVO vo : voList) {
            if (vo.getStatus() != null) {
                vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
            }
        }
    }

    /**
     * 查询所有 status=1 的产品，每个产品携带 status=1 的规格列表，供打印信息选项接口使用
     * 无规格的产品仍返回（specs 为空列表）
     *
     * @return 产品列表（含 specs 字段）
     */
    @Override
    public List<ProductVO> listAllWithSpecs() {
        log.info("查询所有产品（含规格），用于打印信息选项");
        try {
            // 1. 查所有 status=1 的产品
            List<ProductEntity> products = list(new LambdaQueryWrapper<ProductEntity>()
                    .eq(ProductEntity::getStatus, StatusConstants.NORMAL)
                    .orderByAsc(ProductEntity::getId));

            if (products.isEmpty()) {
                return Collections.emptyList();
            }

            // 2. 批量查询所有产品下 status=1 的规格，避免 N+1
            List<Long> productIds = products.stream().map(ProductEntity::getId).toList();
            List<ProductSpecEntity> allSpecs = productSpecService.list(
                    new LambdaQueryWrapper<ProductSpecEntity>()
                            .in(ProductSpecEntity::getProductId, productIds)
                            .eq(ProductSpecEntity::getStatus, StatusConstants.NORMAL)
                            .orderByAsc(ProductSpecEntity::getSort)
                            .orderByAsc(ProductSpecEntity::getId));

            // 3. 按 productId 分组
            Map<Long, List<ProductSpecEntity>> specMap = allSpecs.stream()
                    .collect(Collectors.groupingBy(ProductSpecEntity::getProductId));

            // 4. 组装 VO
            return products.stream().map(product -> {
                ProductVO vo = ProductConvert.toVO(product);
                vo.setStatusName(StatusConstants.getStatusName(product.getStatus()));
                List<ProductSpecEntity> specs = specMap.getOrDefault(product.getId(), Collections.emptyList());
                List<ProductSpecVO> specVOs = specs.stream().map(spec -> {
                    ProductSpecVO specVO = new ProductSpecVO();
                    BeanUtils.copyProperties(spec, specVO);
                    return specVO;
                }).toList();
                vo.setSpecs(specVOs);
                return vo;
            }).toList();
        } catch (Exception e) {
            log.error("查询所有产品（含规格）异常", e);
            throw e;
        }
    }
}
