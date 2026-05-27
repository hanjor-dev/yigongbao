package com.yigongbao.module.basic.device.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.event.DeviceStateChangeEvent;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.module.basic.device.convert.DeviceConvert;
import com.yigongbao.module.basic.device.dto.CreateDeviceDTO;
import com.yigongbao.module.basic.device.dto.DevicePageDTO;
import com.yigongbao.module.basic.device.dto.DeviceStatusPushDTO;
import com.yigongbao.module.basic.device.dto.UpdateDeviceDTO;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.entity.DeviceStateLogEntity;
import com.yigongbao.module.basic.device.enums.DeviceTypeEnum;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.basic.device.service.IDeviceService;
import com.yigongbao.module.basic.device.service.IDeviceStateLogService;
import com.yigongbao.module.basic.device.vo.DeviceVO;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.mapper.ProcessingCenterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备管理服务实现类
 *
 * 负责设备的CRUD操作、状态管理、WebSocket批量更新、离线检测等功能
 *
 * @author hanjor
 * @date 2026-05-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, DeviceEntity> implements IDeviceService {

    private final ProcessingCenterMapper processingCenterMapper;
    private final IDeviceStateLogService deviceStateLogService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 分页查询设备列表
     *
     * @param dto 分页查询参数（支持按中心、类型、状态、连接状态、设备编号筛选）
     * @return 分页结果
     */
    @Override
    public IPage<DeviceVO> listDevices(DevicePageDTO dto) {
        // 构建查询条件
        LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dto.getCenterId() != null, DeviceEntity::getCenterId, dto.getCenterId())
               .eq(StrUtil.isNotBlank(dto.getDeviceType()), DeviceEntity::getDeviceType, dto.getDeviceType())
               .eq(dto.getState() != null, DeviceEntity::getState, dto.getState())
               .eq(dto.getConnectionStatus() != null, DeviceEntity::getConnectionStatus, dto.getConnectionStatus())
               .like(StrUtil.isNotBlank(dto.getDeviceId()), DeviceEntity::getDeviceId, dto.getDeviceId())
               .orderByDesc(DeviceEntity::getUpdateTime);

        IPage<DeviceEntity> page = page(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        return page.convert(DeviceConvert::toVO);
    }

    /**
     * 根据ID查询设备详情
     *
     * @param id 设备ID
     * @return 设备详情
     * @throws BusinessException 数据不存在时抛出
     */
    @Override
    public DeviceVO getDeviceById(Long id) {
        DeviceEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        return DeviceConvert.toVO(entity);
    }

    /**
     * 手动创建设备
     *
     * @param dto 创建参数
     * @return 新创建的设备ID
     * @throws BusinessException 设备编号已存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDevice(CreateDeviceDTO dto) {
        if (StrUtil.isNotBlank(dto.getDeviceType())) {
            boolean autoRegistered = Arrays.stream(DeviceTypeEnum.values())
                    .anyMatch(e -> e.getCode().equals(dto.getDeviceType()) && e.isAutoRegistered());
            if (autoRegistered) {
                throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "3D打印类设备不允许手动创建，请通过设备端WebSocket接入");
            }
        }
        LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceEntity::getDeviceId, dto.getDeviceId());
        if (count(wrapper) > 0) {
            throw new BusinessException(ErrorCodeEnum.DATA_EXISTS, "设备编号已存在");
        }

        DeviceEntity entity = DeviceConvert.toEntity(dto);
        if (dto.getCenterId() != null) {
            ProcessingCenterEntity center = processingCenterMapper.selectById(dto.getCenterId());
            if (center == null) {
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND, "加工中心");
            }
            entity.setCenterName(center.getCenterName());
        }
        save(entity);

        log.info("创建设备: id={}, deviceId={}, deviceType={}",
            entity.getId(), entity.getDeviceId(), entity.getDeviceType());

        return entity.getId();
    }

    /**
     * 手动更新设备状态
     *
     * @param id 设备ID
     * @param state 新状态（0=空闲，1=占用）
     * @throws BusinessException 数据不存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDeviceState(Long id, Integer state) {
        DeviceEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        Integer oldState = entity.getState();
        entity.setState(state);
        updateById(entity);

        // 状态发生变化时记录状态变更日志
        if (!oldState.equals(state)) {
            DeviceStateLogEntity log = new DeviceStateLogEntity();
            log.setDeviceId(entity.getDeviceId());
            log.setOldState(oldState);
            log.setNewState(state);
            log.setChangeTime(LocalDateTime.now());
            log.setChangeType("manual");
            deviceStateLogService.save(log);
        }

        log.info("更新设备状态: deviceId={}, {} -> {}", entity.getDeviceId(), oldState, state);
    }

    /**
     * 查询指定加工中心下某类型的所有设备（用于任务分配）
     *
     * @param centerId   加工中心ID（可选）
     * @param deviceType 设备类型（可选）
     * @return 设备列表
     */
    @Override
    public List<DeviceVO> listDevicesByCenterAndType(Long centerId, String deviceType) {
        LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(centerId != null, DeviceEntity::getCenterId, centerId)
               .eq(StrUtil.isNotBlank(deviceType), DeviceEntity::getDeviceType, deviceType)
               .orderByAsc(DeviceEntity::getDeviceId);

        return list(wrapper).stream()
                .map(DeviceConvert::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 批量更新设备状态（WebSocket推送触发）
     *
     * 功能说明：
     * 1. 根据加工中心名称查询加工中心信息
     * 2. 遍历设备列表，自动创建不存在的设备或更新已有设备状态
     * 3. 记录状态变更日志
     *
     * @param dto WebSocket推送的设备状态数据（包含加工中心名称和设备列表）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateDeviceStatus(DeviceStatusPushDTO dto) {
        LambdaQueryWrapper<ProcessingCenterEntity> centerWrapper = new LambdaQueryWrapper<>();
        centerWrapper.eq(ProcessingCenterEntity::getCenterName, dto.getCenterName())
                     .eq(ProcessingCenterEntity::getStatus, StatusConstants.NORMAL);
        ProcessingCenterEntity center = processingCenterMapper.selectOne(centerWrapper);

        if (center == null) {
            log.warn("加工中心不存在或已禁用: centerName={}", dto.getCenterName());
            return false;
        }

        // 解析该中心的设备ID范围，用于过滤越界设备
        List<String[]> allowedRanges = parseDeviceIdRanges(center.getDeviceIdRanges());

        List<String> deviceIds = dto.getDevices().stream()
                .map(DeviceStatusPushDTO.DeviceStatus::getId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(DeviceEntity::getDeviceId, deviceIds);
        List<DeviceEntity> existingDevices = list(wrapper);
        Map<String, DeviceEntity> deviceMap = existingDevices.stream()
                .collect(Collectors.toMap(DeviceEntity::getDeviceId, d -> d));

        List<DeviceEntity> toCreate = new ArrayList<>();
        List<DeviceEntity> toUpdate = new ArrayList<>();
        List<DeviceStateLogEntity> stateLogs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (DeviceStatusPushDTO.DeviceStatus deviceStatus : dto.getDevices()) {
            // 校验设备ID是否在该中心的允许范围内
            if (!allowedRanges.isEmpty() && !isDeviceIdInRanges(deviceStatus.getId(), allowedRanges)) {
                log.warn("设备ID不在加工中心允许范围内，跳过: deviceId={}, centerName={}", deviceStatus.getId(), dto.getCenterName());
                continue;
            }
            DeviceEntity device = deviceMap.get(deviceStatus.getId());

            if (device == null) {
                device = new DeviceEntity();
                device.setDeviceId(deviceStatus.getId());
                device.setDeviceName(deviceStatus.getId());
                device.setDeviceType(DeviceTypeEnum.PRINTER_SLA.getCode());
                device.setCenterId(center.getId());
                device.setCenterName(center.getCenterName());
                device.setState(deviceStatus.getState());
                device.setConnectionStatus(1);
                device.setLastHeartbeat(now);
                toCreate.add(device);
            } else {
                Integer oldState = device.getState();
                device.setState(deviceStatus.getState());
                device.setConnectionStatus(1);
                device.setLastHeartbeat(now);
                toUpdate.add(device);

                if (!oldState.equals(deviceStatus.getState())) {
                    DeviceStateLogEntity stateLog = new DeviceStateLogEntity();
                    stateLog.setDeviceId(device.getDeviceId());
                    stateLog.setOldState(oldState);
                    stateLog.setNewState(deviceStatus.getState());
                    stateLog.setChangeTime(now);
                    stateLog.setChangeType("auto");
                    stateLogs.add(stateLog);
                    eventPublisher.publishEvent(new DeviceStateChangeEvent(this, device.getId(), oldState, deviceStatus.getState()));
                }
            }
        }

        if (!toCreate.isEmpty()) {
            saveBatch(toCreate);
        }
        for (DeviceEntity device : toUpdate) {
            updateById(device);
        }
        if (!stateLogs.isEmpty()) {
            deviceStateLogService.saveBatch(stateLogs);
        }

        log.info("批量更新设备状态: centerName={}, deviceCount={}, 新增={}, 更新={}",
            dto.getCenterName(), dto.getDevices().size(), toCreate.size(), toUpdate.size());
        return true;
    }

    /**
     * 标记加工中心的所有设备为离线状态
     *
     * @param centerId 加工中心ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markDevicesOffline(Long centerId) {
        LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceEntity::getCenterId, centerId)
               .eq(DeviceEntity::getConnectionStatus, 1);

        DeviceEntity updateEntity = new DeviceEntity();
        updateEntity.setConnectionStatus(0);
        boolean updated = update(updateEntity, wrapper);
        if (updated) {
            log.info("标记加工中心设备离线: centerId={}", centerId);
        }
    }

    /**
     * 解析 deviceIdRanges JSON 为 [start, end] 对列表
     * 格式：[{"start":"P001","end":"P099"}]
     */
    private List<String[]> parseDeviceIdRanges(String deviceIdRanges) {
        if (StrUtil.isBlank(deviceIdRanges)) {
            return java.util.Collections.emptyList();
        }
        try {
            JSONArray arr = JSONUtil.parseArray(deviceIdRanges);
            List<String[]> result = new ArrayList<>();
            for (Object obj : arr) {
                JSONObject item = (JSONObject) obj;
                String start = item.getStr("start");
                String end = item.getStr("end");
                if (StrUtil.isNotBlank(start) && StrUtil.isNotBlank(end)) {
                    result.add(new String[]{start, end});
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("解析 deviceIdRanges 失败，跳过范围校验: {}", deviceIdRanges);
            return java.util.Collections.emptyList();
        }
    }

    /** 判断 deviceId 是否落在任意一个允许范围内（字典序比较） */
    private boolean isDeviceIdInRanges(String deviceId, List<String[]> ranges) {
        for (String[] range : ranges) {
            if (deviceId.compareTo(range[0]) >= 0 && deviceId.compareTo(range[1]) <= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 编辑设备信息
     *
     * @param dto 编辑参数（不允许修改设备编号和设备类型）
     * @throws BusinessException 设备不存在或加工中心不存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDevice(UpdateDeviceDTO dto) {
        DeviceEntity entity = getById(dto.getId());
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        if (dto.getCenterId() != null && !dto.getCenterId().equals(entity.getCenterId())) {
            ProcessingCenterEntity center = processingCenterMapper.selectById(dto.getCenterId());
            if (center == null) {
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND, "加工中心");
            }
            entity.setCenterId(dto.getCenterId());
            entity.setCenterName(center.getCenterName());
        }

        if (StrUtil.isNotBlank(dto.getDeviceName())) {
            entity.setDeviceName(dto.getDeviceName());
        }
        if (dto.getProcessingMinutes() != null) {
            entity.setProcessingMinutes(dto.getProcessingMinutes());
        }
        if (dto.getRemark() != null) {
            entity.setRemark(dto.getRemark());
        }

        updateById(entity);
        log.info("编辑设备信息: id={}, deviceId={}", entity.getId(), entity.getDeviceId());
    }

    /**
     * 删除设备
     *
     * @param id 设备ID
     * @throws BusinessException 设备不存在或设备处于占用状态时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDevice(Long id) {
        DeviceEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        // 占用中的设备不允许删除
        if (Integer.valueOf(1).equals(entity.getState())) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "设备当前处于占用状态，不允许删除");
        }

        removeById(id);
        log.info("删除设备: id={}, deviceId={}", entity.getId(), entity.getDeviceId());
    }
}
