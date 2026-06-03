package com.yigongbao.module.order.service.impl;

import com.yigongbao.module.order.service.IClassicCaseFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 经典案例文件迁移服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassicCaseFileServiceImpl implements IClassicCaseFileService {

    @Override
    public List<String> collectOrderFileIds(Long orderId) {
        List<String> fileIds = new ArrayList<>();

        // TODO: 实现8类文件来源的收集逻辑
        // 1. order_file
        // 2. design_package
        // 3. design_package_file
        // 4. design_package_file_screenshot
        // 5. design_model
        // 6. design_instruction (template_file_id + revised_file_id)
        // 7. design_drawing (template_file_id + revised_file_id)

        return fileIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void migrateFilesToClassicCase(Long orderId, String orderCode) {
        log.info("开始迁移经典案例文件: orderId={}, orderCode={}", orderId, orderCode);

        // TODO: 实现文件迁移逻辑
        // 方式一: 通过file_detail表迁移(1-7类)
        // 方式二: 通过URL迁移(production_record)

        log.info("经典案例文件迁移完成: orderId={}, orderCode={}", orderId, orderCode);
    }
}
