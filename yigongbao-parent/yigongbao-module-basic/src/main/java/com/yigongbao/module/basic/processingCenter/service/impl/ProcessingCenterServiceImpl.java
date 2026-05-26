package com.yigongbao.module.basic.processingCenter.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.module.basic.processingCenter.convert.ProcessingCenterConvert;
import com.yigongbao.module.basic.processingCenter.dto.CreateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.dto.ProcessingCenterPageDTO;
import com.yigongbao.module.basic.processingCenter.dto.UpdateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.mapper.ProcessingCenterMapper;
import com.yigongbao.module.basic.processingCenter.service.IProcessingCenterService;
import com.yigongbao.module.basic.processingCenter.vo.ProcessingCenterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 加工中心管理服务实现类
 *
 * @author hanjor
 * @date 2026-05-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingCenterServiceImpl extends ServiceImpl<ProcessingCenterMapper, ProcessingCenterEntity>
        implements IProcessingCenterService {

    /**
     * 分页查询加工中心列表
     *
     * @param dto 分页查询参数（支持按名称模糊查询、按状态筛选）
     * @return 分页结果
     */
    @Override
    public IPage<ProcessingCenterVO> listProcessingCenters(ProcessingCenterPageDTO dto) {
        // 构建查询条件
        LambdaQueryWrapper<ProcessingCenterEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(dto.getCenterName()), ProcessingCenterEntity::getCenterName, dto.getCenterName())
               .eq(dto.getStatus() != null, ProcessingCenterEntity::getStatus, dto.getStatus())
               .orderByDesc(ProcessingCenterEntity::getCreateTime);

        IPage<ProcessingCenterEntity> page = page(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        return page.convert(ProcessingCenterConvert::toVO);
    }

    /**
     * 根据ID查询加工中心详情
     *
     * @param id 加工中心ID
     * @return 加工中心详情
     * @throws BusinessException 数据不存在时抛出
     */
    @Override
    public ProcessingCenterVO getProcessingCenterById(Long id) {
        ProcessingCenterEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        return ProcessingCenterConvert.toVO(entity);
    }

    /**
     * 创建加工中心
     *
     * @param dto 创建参数
     * @return 新创建的加工中心ID
     * @throws BusinessException 中心编码已存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProcessingCenter(CreateProcessingCenterDTO dto) {
        LambdaQueryWrapper<ProcessingCenterEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessingCenterEntity::getCenterCode, dto.getCenterCode());
        if (count(wrapper) > 0) {
            throw new BusinessException(ErrorCodeEnum.DATA_EXISTS);
        }

        if (StrUtil.isNotBlank(dto.getDeviceIdRanges())) {
            validateDeviceIdRangesNoOverlap(dto.getDeviceIdRanges(), null);
        }

        ProcessingCenterEntity entity = ProcessingCenterConvert.toEntity(dto);
        entity.setStatus(StatusConstants.NORMAL);
        save(entity);

        log.info("创建加工中心: id={}, centerCode={}, centerName={}",
            entity.getId(), entity.getCenterCode(), entity.getCenterName());

        return entity.getId();
    }

    /**
     * 更新加工中心信息
     *
     * @param dto 更新参数（仅更新非空字段）
     * @throws BusinessException 数据不存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProcessingCenter(UpdateProcessingCenterDTO dto) {
        ProcessingCenterEntity entity = getById(dto.getId());
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 仅更新非空字段，避免覆盖原有数据
        if (StrUtil.isNotBlank(dto.getCenterName())) {
            entity.setCenterName(dto.getCenterName());
        }
        if (StrUtil.isNotBlank(dto.getContactPerson())) {
            entity.setContactPerson(dto.getContactPerson());
        }
        if (StrUtil.isNotBlank(dto.getContactPhone())) {
            entity.setContactPhone(dto.getContactPhone());
        }
        if (StrUtil.isNotBlank(dto.getAddress())) {
            entity.setAddress(dto.getAddress());
        }
        if (StrUtil.isNotBlank(dto.getDeviceIdRanges())) {
            validateDeviceIdRangesNoOverlap(dto.getDeviceIdRanges(), dto.getId());
            entity.setDeviceIdRanges(dto.getDeviceIdRanges());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (StrUtil.isNotBlank(dto.getRemark())) {
            entity.setRemark(dto.getRemark());
        }

        updateById(entity);
        log.info("更新加工中心: id={}, centerCode={}", entity.getId(), entity.getCenterCode());
    }

    /**
     * 删除加工中心（逻辑删除）
     *
     * @param id 加工中心ID
     * @throws BusinessException 数据不存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcessingCenter(Long id) {
        ProcessingCenterEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        removeById(id);
        log.info("删除加工中心: id={}, centerCode={}", id, entity.getCenterCode());
    }

    /**
     * 查询所有启用的加工中心（用于下拉选择）
     *
     * @return 启用的加工中心列表，按编码升序排列
     */
    @Override
    public List<ProcessingCenterVO> listAllEnabled() {
        // 查询状态为启用的加工中心
        LambdaQueryWrapper<ProcessingCenterEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessingCenterEntity::getStatus, StatusConstants.NORMAL)
               .orderByAsc(ProcessingCenterEntity::getCenterCode);

        return list(wrapper).stream()
                .map(ProcessingCenterConvert::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 校验设备ID范围与其他加工中心不重叠
     * <p>
     * deviceIdRanges 格式：[{"start":"P001","end":"P099"}]
     * 范围比较基于字符串字典序，要求 start <= end，且与其他中心的任意范围无交叉。
     *
     * @param deviceIdRanges 待校验的范围 JSON 字符串
     * @param excludeId      更新时排除自身 ID，创建时传 null
     */
    private void validateDeviceIdRangesNoOverlap(String deviceIdRanges, Long excludeId) {
        JSONArray newRanges;
        try {
            newRanges = JSONUtil.parseArray(deviceIdRanges);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "deviceIdRanges 格式不合法");
        }

        // 收集新范围的 [start, end] 对
        List<String[]> newPairs = new ArrayList<>();
        for (Object obj : newRanges) {
            JSONObject item = (JSONObject) obj;
            String start = item.getStr("start");
            String end = item.getStr("end");
            if (StrUtil.isBlank(start) || StrUtil.isBlank(end) || start.compareTo(end) > 0) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "设备ID范围 start 必须 <= end");
            }
            newPairs.add(new String[]{start, end});
        }

        if (newPairs.isEmpty()) {
            return;
        }

        // 查询其他所有加工中心的范围
        LambdaQueryWrapper<ProcessingCenterEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(ProcessingCenterEntity::getDeviceIdRanges)
               .ne(ProcessingCenterEntity::getDeviceIdRanges, "");
        if (excludeId != null) {
            wrapper.ne(ProcessingCenterEntity::getId, excludeId);
        }

        for (ProcessingCenterEntity other : list(wrapper)) {
            if (StrUtil.isBlank(other.getDeviceIdRanges())) continue;
            JSONArray otherRanges;
            try {
                otherRanges = JSONUtil.parseArray(other.getDeviceIdRanges());
            } catch (Exception e) {
                continue;
            }
            for (Object obj : otherRanges) {
                JSONObject item = (JSONObject) obj;
                String os = item.getStr("start");
                String oe = item.getStr("end");
                if (StrUtil.isBlank(os) || StrUtil.isBlank(oe)) continue;
                for (String[] np : newPairs) {
                    // 两个区间 [a,b] 和 [c,d] 不重叠的条件：b < c 或 d < a
                    if (!(np[1].compareTo(os) < 0 || oe.compareTo(np[0]) < 0)) {
                        log.warn("设备ID范围与加工中心[{}]存在重叠: newRange=[{},{}], existingRange=[{},{}]",
                            other.getCenterCode(), np[0], np[1], os, oe);
                        throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,
                            "设备ID范围与加工中心[" + other.getCenterName() + "]存在重叠");
                    }
                }
            }
        }
    }
}
