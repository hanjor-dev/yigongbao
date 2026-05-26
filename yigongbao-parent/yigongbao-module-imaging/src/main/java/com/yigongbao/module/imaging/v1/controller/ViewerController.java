package com.yigongbao.module.imaging.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.imaging.v1.service.ViewerService;
import com.yigongbao.module.imaging.v1.vo.ViewerConfigVO;
import com.yigongbao.module.imaging.v1.vo.ViewerStlVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 影像查看器适配接口（v1）
 *
 * @author hanjor
 * @date 2026-05-06
 */
@Tag(name = "影像查看器v1", description = "为前端通用3D影像查看器提供适配接口")
@SaCheckLogin
@RestController
@RequestMapping("/imaging/v1")
@RequiredArgsConstructor
public class ViewerController {

    private final ViewerService viewerService;

    @Operation(summary = "获取查看器初始化配置（paths + token）")
    @GetMapping("/config")
    public Result<ViewerConfigVO> getConfig(@RequestParam Long orderId) {
        String token = StpUtil.getTokenValue();
        return Result.success(viewerService.getViewerConfig(orderId, token));
    }

    @SaIgnore
    @Operation(summary = "dcmPath - DCM影像压缩包URL列表")
    @PostMapping("/dcm")
    public Result<List<String>> getDcm(@RequestBody Map<String, Object> params) {
        if (params == null || params.get("orderId") == null) {
            throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "orderId");
        }
        Long orderId = Long.valueOf(params.get("orderId").toString());
        return Result.success(viewerService.getDcmUrls(orderId));
    }

    @SaIgnore
    @Operation(summary = "stlPath - STL模型数据（按数据包分组）")
    @PostMapping("/stl")
    public Result<ViewerStlVO> getStl(@RequestBody Map<String, Object> params) {
        if (params == null || params.get("orderId") == null) {
            throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "orderId");
        }
        Long orderId = Long.valueOf(params.get("orderId").toString());
        return Result.success(viewerService.getStlData(orderId));
    }

    @SaIgnore
    @Operation(summary = "markPath - 提交标注截图")
    @PostMapping("/mark")
    public Result<Void> saveMark(
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String id,
            @RequestPart MultipartFile file) {
        viewerService.saveMark(groupId, id, file);
        return Result.success();
    }
}
