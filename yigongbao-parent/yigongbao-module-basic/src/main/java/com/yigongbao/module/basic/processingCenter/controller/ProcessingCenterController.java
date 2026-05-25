package com.yigongbao.module.basic.processingCenter.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.processingCenter.dto.CreateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.dto.ProcessingCenterPageDTO;
import com.yigongbao.module.basic.processingCenter.dto.UpdateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.service.IProcessingCenterService;
import com.yigongbao.module.basic.processingCenter.vo.ProcessingCenterVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/basic/processing-center")
@RequiredArgsConstructor
public class ProcessingCenterController {

    private final IProcessingCenterService processingCenterService;

    @PostMapping("/list")
    public Result<IPage<ProcessingCenterVO>> list(@RequestBody ProcessingCenterPageDTO dto) {
        return Result.success(processingCenterService.listProcessingCenters(dto));
    }

    @GetMapping("/{id}")
    public Result<ProcessingCenterVO> getById(@PathVariable Long id) {
        return Result.success(processingCenterService.getProcessingCenterById(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateProcessingCenterDTO dto) {
        return Result.success(processingCenterService.createProcessingCenter(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateProcessingCenterDTO dto) {
        dto.setId(id);
        processingCenterService.updateProcessingCenter(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        processingCenterService.deleteProcessingCenter(id);
        return Result.success();
    }

    @GetMapping("/all")
    public Result<List<ProcessingCenterVO>> listAll() {
        return Result.success(processingCenterService.listAllEnabled());
    }
}
