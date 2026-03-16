package com.yigongbao.module.system.dict.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.system.dict.dto.CreateDictDTO;
import com.yigongbao.module.system.dict.dto.UpdateDictDTO;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理 Controller
 * 处理字典相关的 HTTP 请求
 *
 * @author hanjor
 * @date 2026-03-16
 */
@RestController
@RequestMapping("/api/system/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    /**
     * 字典类型列表（根节点）
     *
     * @return 字典类型列表
     */
    @GetMapping("/type/list")
    public Result<List<DictVO>> listType() {
        return Result.success(dictService.listType());
    }

    /**
     * 根据类型编码获取字典数据列表
     *
     * @param typeCode 类型编码
     * @return 字典数据列表
     */
    @GetMapping("/data/{typeCode}")
    public Result<List<DictVO>> listByTypeCode(@PathVariable String typeCode) {
        return Result.success(dictService.listByTypeCode(typeCode));
    }

    /**
     * 获取完整树形结构
     *
     * @return 树形结构列表
     */
    @GetMapping("/tree")
    public Result<List<DictVO>> listTree() {
        return Result.success(dictService.listTree());
    }

    /**
     * 获取指定类型的树形结构
     *
     * @param typeCode 类型编码
     * @return 树形结构列表
     */
    @GetMapping("/tree/{typeCode}")
    public Result<List<DictVO>> listTreeByTypeCode(@PathVariable String typeCode) {
        return Result.success(dictService.listTreeByTypeCode(typeCode));
    }

    /**
     * 获取下拉选项（叶子节点）
     *
     * @param typeCode 类型编码
     * @return 叶子节点列表
     */
    @GetMapping("/options/{typeCode}")
    public Result<List<DictVO>> listOptions(@PathVariable String typeCode) {
        return Result.success(dictService.listOptions(typeCode));
    }

    /**
     * 根据ID查询字典
     *
     * @param id 字典ID
     * @return 字典详情
     */
    @GetMapping("/{id}")
    public Result<DictVO> getById(@PathVariable Long id) {
        return Result.success(dictService.getById(id));
    }

    /**
     * 创建字典
     *
     * @param dto 创建参数
     * @return 创建结果
     */
    @PostMapping
    public Result<Void> create(@Validated @RequestBody CreateDictDTO dto) {
        dictService.create(dto);
        return Result.success();
    }

    /**
     * 更新字典
     *
     * @param id 字典ID
     * @param dto 更新参数
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody UpdateDictDTO dto) {
        dictService.update(id, dto);
        return Result.success();
    }

    /**
     * 删除字典
     *
     * @param id 字典ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        dictService.remove(id);
        return Result.success();
    }

    /**
     * 修改字典状态（级联）
     *
     * @param id 字典ID
     * @param status 状态值（0=禁用，1=正常）
     * @return 操作结果
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        dictService.updateStatus(id, status);
        return Result.success();
    }
}
