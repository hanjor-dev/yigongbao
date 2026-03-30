package com.yigongbao.module.system.basedata.controller;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.yigongbao.module.basic.area.service.AreaService;
import com.yigongbao.module.basic.area.vo.AreaVO;
import com.yigongbao.module.system.basedata.vo.SelectTreeVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
import lombok.RequiredArgsConstructor;
import cn.hutool.core.util.StrUtil;
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
 * 提供统一的树形结构和下拉选项接口，整合字典、地区、配置分组等数据
 * <p>
 * type 类型说明：
 * <ul>
 *   <li>tree 接口支持的 type：
 *     <ul>
 *       <li>area - 地区树形结构（需传 parentId 参数）</li>
 *       <li>dict - 字典树形结构（需传 code 参数）</li>
 *     </ul>
 *   </li>
 *   <li>options 接口支持的 type：
 *     <ul>
 *       <li>area - 地区下拉选项（需传 parentId 参数）</li>
 *       <li>dict - 字典下拉选项（需传 code 参数）</li>
 *       <li>config_group - 配置分组下拉选项（无需额外参数）</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Tag(name = "基础数据下拉选项", description = "各模块下拉列表数据接口")
@RestController
@RequestMapping("/system/select")
@RequiredArgsConstructor
public class SelectController {

    private final AreaService areaService;
    private final DictService dictService;
    private final ConfigService configService;

    /**
     * 获取树形结构
     * <p>
     * type 支持的类型：
     * <ul>
     *   <li>area - 地区树形结构（需传 parentId 参数，父级ID，默认0）</li>
     *   <li>dict - 字典树形结构（需传 code 参数，字典编码，如：1、2、3 等）</li>
     * </ul>
     *
     * @param type 数据类型（area=地区，dict=字典）
     * @param code 字典编码（type=dict时必填）
     * @param parentId 父级ID（type=area时必填）
     * @return 树形结构列表
     */
    @Operation(summary = "获取树形结构")
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
            if (!StrUtil.isNotBlank(code)) {
                throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "code");
            }
            List<DictVO> dictTree = dictService.listTreeByTypeCode(code);
            return Result.success(convertDictToSelectTree(dictTree));
        }
        throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "不支持的数据类型：" + type);
    }

    /**
     * 获取下拉选项（叶子节点）
     * <p>
     * type 支持的类型：
     * <ul>
     *   <li>area - 地区下拉选项（需传 parentId 参数，父级ID，默认0）</li>
     *   <li>dict - 字典下拉选项（需传 code 参数，字典编码，如：1、2、3 等）</li>
     *   <li>config_group - 配置分组下拉选项（预设分组：系统配置、安全配置、其他配置）</li>
     * </ul>
     *
     * @param type 数据类型（area=地区，dict=字典，config_group=配置分组）
     * @param code 字典编码（type=dict时必填）
     * @param parentId 父级ID（type=area时必填）
     * @return 下拉选项列表
     */
    @Operation(summary = "获取下拉选项（叶子节点）")
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
            if (!StrUtil.isNotBlank(code)) {
                throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "code");
            }
            List<DictVO> dictOptions = dictService.listOptions(code);
            return Result.success(convertDictToSelectTree(dictOptions));
        } else if ("config_group".equals(type)) {
            // 配置分组下拉选项
            List<SelectTreeVO> configGroups = configService.listConfigGroups();
            return Result.success(configGroups);
        }
        throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "不支持的数据类型：" + type);
    }

    /**
     * 获取文件业务类型下拉列表
     * <p>
     * 返回所有启用的文件业务类型，供前端文件上传时 bizType 下拉选择
     *
     * @return 文件业务类型列表（name=字典名称, value=dictCode）
     */
    @Operation(summary = "获取文件业务类型下拉列表")
    @GetMapping("/biz-type-list")
    public Result<List<SelectTreeVO>> listBizTypes() {
        List<DictVO> bizTypes = dictService.listFileBizTypeOptions();
        return Result.success(convertDictToSelectTree(bizTypes));
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
