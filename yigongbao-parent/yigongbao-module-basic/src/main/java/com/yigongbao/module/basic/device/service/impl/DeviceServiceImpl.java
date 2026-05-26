package com.yigongbao.module.basic.device.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.module.basic.device.convert.DeviceConvert;
import com.yigongbao.module.basic.device.dto.CreateDeviceDTO;
import com.yigongbao.module.basic.device.dto.DevicePageDTO;
import com.yigongbao.module.basic.device.dto.DeviceStatusPushDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
        LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceEntity::getDeviceId, dto.getDeviceId());
        if (count(wrapper) > 0) {
            throw new BusinessException(ErrorCodeEnum.DATA_EXISTS, "设备编号已存在");
        }

        DeviceEntity entity = DeviceConvert.toEntity(dto);
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
    public void batchUpdateDeviceStatus(DeviceStatusPushDTO dto) {
        LambdaQueryWrapper<ProcessingCenterEntity> centerWrapper = new LambdaQueryWrapper<>();
        centerWrapper.eq(ProcessingCenterEntity::getCenterName, dto.getCenterName())
                     .eq(ProcessingCenterEntity::getStatus, StatusConstants.NORMAL);
        ProcessingCenterEntity center = processingCenterMapper.selectOne(centerWrapper);

        if (center == null) {
            log.warn("加工中心不存在或已禁用: centerName={}", dto.getCenterName());
            return;
        }

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
     * 检测离线设备（定时任务调用）
     *
     * 检测规则：最后心跳时间超过5分钟的在线设备标记为离线
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void detectOfflineDevices() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceEntity::getConnectionStatus, 1)
               .lt(DeviceEntity::getLastHeartbeat, threshold);

        long offlineCount = count(wrapper);
        if (offlineCount > 0) {
            DeviceEntity updateEntity = new DeviceEntity();
            updateEntity.setConnectionStatus(0);
            update(updateEntity, wrapper);
            log.info("离线检测完成: 检测到{}个离线设备", offlineCount);
        }
    }
}
