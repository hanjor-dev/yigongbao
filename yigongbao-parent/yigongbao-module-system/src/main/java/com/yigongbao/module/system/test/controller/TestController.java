package com.yigongbao.module.system.test.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.system.test.dto.CreateTestDTO;
import com.yigongbao.module.system.test.dto.UpdateTestDTO;
import com.yigongbao.module.system.test.service.TestService;
import com.yigongbao.module.system.test.vo.TestVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 测试 Controller
 * 处理测试相关的 HTTP 请求
 *
 * @author hanjor
 * @date 2026-03-14 18:25:00
 */
@RequestMapping("/test")
@RestController
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    /**
     * 查询所有测试数据
     *
     * @return 测试数据列表
     */
    @GetMapping("/list")
    public Result<List<TestVO>> list() {
        return Result.success(testService.listVo());
    }

    /**
     * 根据ID查询测试数据
     *
     * @param id 主键ID
     * @return 测试数据
     */
    @GetMapping("/{id}")
    public Result<TestVO> getById(@PathVariable Long id) {
        return Result.success(testService.getVoById(id));
    }

    /**
     * 创建测试数据
     *
     * @param dto 创建参数
     * @return 创建结果
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateTestDTO dto) {
        testService.create(dto);
        return Result.success();
    }

    /**
     * 更新测试数据
     *
     * @param id  主键ID
     * @param dto 更新参数
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateTestDTO dto) {
        testService.update(id, dto);
        return Result.success();
    }

    /**
     * 删除测试数据
     *
     * @param id 主键ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        testService.remove(id);
        return Result.success();
    }
}
