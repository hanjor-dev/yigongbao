package com.yigongbao.module.basic.registrationCert.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.module.basic.registrationCert.dto.CreateRegistrationCertDTO;
import com.yigongbao.module.basic.registrationCert.dto.RegistrationCertPageDTO;
import com.yigongbao.module.basic.registrationCert.dto.UpdateRegistrationCertDTO;
import com.yigongbao.module.basic.registrationCert.service.RegistrationCertService;
import com.yigongbao.module.basic.registrationCert.vo.RegistrationCertVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 注册证管理 Controller
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Tag(name = "注册证管理", description = "产品注册证管理")
@RestController
@RequestMapping("/basic/registration-cert")
@RequiredArgsConstructor
public class RegistrationCertController {

    private final RegistrationCertService registrationCertService;

    /**
     * 分页查询注册证列表
     */
    @Operation(summary = "分页查询注册证列表")
    @PostMapping("/list")
    public Result<IPage<RegistrationCertVO>> list(@Validated @RequestBody RegistrationCertPageDTO dto) {
        return Result.success(registrationCertService.listCerts(dto));
    }

    /**
     * 根据ID查询注册证详情
     *
     * @param id 注册证ID
     * @return 注册证详情
     */
    @Operation(summary = "根据ID查询注册证详情")
    @GetMapping("/{id}")
    public Result<RegistrationCertVO> getById(@PathVariable Long id) {
        return Result.success(registrationCertService.getById(id));
    }

    /**
     * 创建注册证
     *
     * @param dto 创建参数
     * @return 操作结果
     */
    @Operation(summary = "创建注册证")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "创建注册证"
    )
    @PostMapping
    public Result<Void> create(@Validated @RequestBody CreateRegistrationCertDTO dto) {
        registrationCertService.create(dto);
        return Result.success();
    }

    /**
     * 更新注册证信息
     *
     * @param id 注册证ID
     * @param dto 更新参数
     * @return 操作结果
     */
    @Operation(summary = "更新注册证信息")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新注册证"
    )
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody UpdateRegistrationCertDTO dto) {
        registrationCertService.update(id, dto);
        return Result.success();
    }

    /**
     * 删除注册证
     *
     * @param id 注册证ID
     * @return 操作结果
     */
    @Operation(summary = "删除注册证")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除注册证"
    )
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        registrationCertService.remove(id);
        return Result.success();
    }

    /**
     * 查询有效注册证列表（有效期未过期的）
     *
     * @return 有效注册证列表
     */
    @Operation(summary = "查询有效注册证列表")
    @GetMapping("/valid-list")
    public Result<List<RegistrationCertVO>> validList() {
        return Result.success(registrationCertService.listValidCerts());
    }
}
