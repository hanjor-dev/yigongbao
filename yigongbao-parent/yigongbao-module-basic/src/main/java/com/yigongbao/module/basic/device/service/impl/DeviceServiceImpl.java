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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, DeviceEntity> implements IDeviceService {

    private final ProcessingCenterMapper processingCenterMapper;
    private final IDeviceStateLogService deviceStateLogService;

    @Override
    public IPage<DeviceVO> listDevices(DevicePageDTO dto) {
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

    @Override
    public DeviceVO getDeviceById(Long id) {
        DeviceEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        return DeviceConvert.toVO(entity);
    }

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

    @Override
    public List<DeviceVO> listIdleDevices(Long centerId, String deviceType) {
        LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(centerId != null, DeviceEntity::getCenterId, centerId)
               .eq(StrUtil.isNotBlank(deviceType), DeviceEntity::getDeviceType, deviceType)
               .eq(DeviceEntity::getState, 0)
               .eq(DeviceEntity::getConnectionStatus, 1)
               .orderByAsc(DeviceEntity::getDeviceId);

        return list(wrapper).stream()
                .map(DeviceConvert::toVO)
                .collect(Collectors.toList());
    }

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

        for (DeviceStatusPushDTO.DeviceStatus deviceStatus : dto.getDevices()) {
            LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DeviceEntity::getDeviceId, deviceStatus.getId());
            DeviceEntity device = getOne(wrapper);

            if (device == null) {
                device = new DeviceEntity();
                device.setDeviceId(deviceStatus.getId());
                device.setDeviceName(deviceStatus.getId());
                device.setDeviceType(DeviceTypeEnum.PRINTER_SLA.getCode());
                device.setCenterId(center.getId());
                device.setCenterName(center.getCenterName());
                device.setState(deviceStatus.getState());
                device.setConnectionStatus(1);
                device.setLastHeartbeat(LocalDateTime.now());
                save(device);

                log.info("自动创建设备: deviceId={}, centerId={}", device.getDeviceId(), center.getId());
            } else {
                Integer oldState = device.getState();
                device.setState(deviceStatus.getState());
                device.setConnectionStatus(1);
                device.setLastHeartbeat(LocalDateTime.now());
                updateById(device);

                if (!oldState.equals(deviceStatus.getState())) {
                    DeviceStateLogEntity stateLog = new DeviceStateLogEntity();
                    stateLog.setDeviceId(device.getDeviceId());
                    stateLog.setOldState(oldState);
                    stateLog.setNewState(deviceStatus.getState());
                    stateLog.setChangeTime(LocalDateTime.now());
                    stateLog.setChangeType("auto");
                    deviceStateLogService.save(stateLog);
                }
            }
        }

        log.info("批量更新设备状态: centerName={}, deviceCount={}", dto.getCenterName(), dto.getDevices().size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markDevicesOffline(Long centerId) {
        LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceEntity::getCenterId, centerId)
               .eq(DeviceEntity::getConnectionStatus, 1);

        List<DeviceEntity> devices = list(wrapper);
        for (DeviceEntity device : devices) {
            device.setConnectionStatus(0);
            updateById(device);
        }

        log.info("标记加工中心设备离线: centerId={}, deviceCount={}", centerId, devices.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void detectOfflineDevices() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        LambdaQueryWrapper<DeviceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceEntity::getConnectionStatus, 1)
               .lt(DeviceEntity::getLastHeartbeat, threshold);

        List<DeviceEntity> devices = list(wrapper);
        for (DeviceEntity device : devices) {
            device.setConnectionStatus(0);
            updateById(device);
            log.warn("设备离线: deviceId={}, lastHeartbeat={}", device.getDeviceId(), device.getLastHeartbeat());
        }

        if (!devices.isEmpty()) {
            log.info("离线检测完成: 检测到{}个离线设备", devices.size());
        }
    }
}
