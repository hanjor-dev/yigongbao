package com.yigongbao.module.system.basedata.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.system.basedata.area.service.AreaService;
import com.yigongbao.module.system.basedata.area.vo.AreaVO;
import com.yigongbao.module.system.basedata.vo.SelectTreeVO;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 统一下拉查询 Controller
 * 提供统一的树形结构和下拉选项接口，整合字典和地区数据
 *
 * @author hanjor
 * @date 2026-03-17
 */
@RestController
@RequestMapping("/api/system/select")
@RequiredArgsConstructor
public class SelectController {

    private final AreaService areaService;
    private final DictService dictService;

    /**
     * 获取树形结构
     * - 地区：/api/system/select/tree?type=area&parentId=0
     * - 字典：/api/system/select/tree?type=dict&code=user_status
     *
     * @param type 数据类型（area=地区，dict=字典）
     * @param code 字典编码（type=dict时必填）
     * @param parentId 父级ID（type=area时必填）
     * @return 树形结构列表
     */
    @GetMapping("/tree")
    public Result<List<SelectTreeVO>> getTree(
            @RequestParam String type,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Long parentId) {

        if ("area".equals(type)) {
            // 地区树形结构
            if (parentId == null) {
                parentId = 0L;
            }
            List<AreaVO> areaTree = areaService.listTree(parentId);
            return Result.success(convertAreaToSelectTree(areaTree));
        } else if ("dict".equals(type)) {
            // 字典树形结构
            if (!StringUtils.hasText(code)) {
                return Result.error(400, "字典查询需要提供code参数");
            }
            List<DictVO> dictTree = dictService.listTreeByTypeCode(code);
            return Result.success(convertDictToSelectTree(dictTree));
        }
        return Result.error(400, "不支持的数据类型：" + type);
    }

    /**
     * 获取下拉选项（叶子节点）
     * - 地区：/api/system/select/options?type=area&parentId=1
     * - 字典：/api/system/select/options?type=dict&code=hospital_level
     *
     * @param type 数据类型（area=地区，dict=字典）
     * @param code 字典编码（type=dict时必填）
     * @param parentId 父级ID（type=area时必填）
     * @return 下拉选项列表
     */
    @GetMapping("/options")
    public Result<List<SelectTreeVO>> getOptions(
            @RequestParam String type,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Long parentId) {

        if ("area".equals(type)) {
            // 地区下拉选项
            if (parentId == null) {
                parentId = 0L;
            }
            List<AreaVO> areaList = areaService.listByParentId(parentId);
            return Result.success(convertAreaToSelectTree(areaList));
        } else if ("dict".equals(type)) {
            // 字典下拉选项（叶子节点）
            if (!StringUtils.hasText(code)) {
                return Result.error(400, "字典查询需要提供code参数");
            }
            List<DictVO> dictOptions = dictService.listOptions(code);
            return Result.success(convertDictToSelectTree(dictOptions));
        }
        return Result.error(400, "不支持的数据类型：" + type);
    }

    /**
     * 地区VO转换为统一下拉VO
     *
     * @param areaList 地区VO列表
     * @return 统一下拉VO列表
     */
    private List<SelectTreeVO> convertAreaToSelectTree(List<AreaVO> areaList) {
        if (areaList == null || areaList.isEmpty()) {
            return List.of();
        }
        return areaList.stream()
                .map(this::convertAreaToSelectTree)
                .collect(Collectors.toList());
    }

    /**
     * 单个地区VO转换为统一下拉VO（value 使用 area_code）
     *
     * @param areaVO 地区VO
     * @return 统一下拉VO
     */
    private SelectTreeVO convertAreaToSelectTree(AreaVO areaVO) {
        if (areaVO == null) {
            return null;
        }
        SelectTreeVO vo = new SelectTreeVO();
        vo.setId(areaVO.getId());
        vo.setName(areaVO.getName());
        vo.setValue(areaVO.getAreaCode() != null ? String.valueOf(areaVO.getAreaCode()) : null);
        vo.setParentId(areaVO.getParentCode());
        vo.setLevel(areaVO.getLevel());
        if (areaVO.getChildren() != null && !areaVO.getChildren().isEmpty()) {
            vo.setChildren(convertAreaToSelectTree(areaVO.getChildren()));
        }
        return vo;
    }

    /**
     * 字典VO转换为统一下拉VO
     *
     * @param dictList 字典VO列表
     * @return 统一下拉VO列表
     */
    private List<SelectTreeVO> convertDictToSelectTree(List<DictVO> dictList) {
        if (dictList == null || dictList.isEmpty()) {
            return List.of();
        }
        return dictList.stream()
                .map(this::convertDictToSelectTree)
                .collect(Collectors.toList());
    }

    /**
     * 单个字典VO转换为统一下拉VO
     *
     * @param dictVO 字典VO
     * @return 统一下拉VO
     */
    private SelectTreeVO convertDictToSelectTree(DictVO dictVO) {
        if (dictVO == null) {
            return null;
        }
        SelectTreeVO vo = new SelectTreeVO();
        vo.setId(dictVO.getId());
        vo.setName(dictVO.getDictName());
        vo.setValue(dictVO.getDictValue());
        vo.setParentId(dictVO.getParentId());
        vo.setLevel(dictVO.getLevel());
        // 递归转换子节点
        if (dictVO.getChildren() != null && !dictVO.getChildren().isEmpty()) {
            vo.setChildren(convertDictToSelectTree(dictVO.getChildren()));
        }
        return vo;
    }
}
