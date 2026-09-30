package com.yigongbao.module.notification.announcement.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.RequirePermission;
import com.yigongbao.module.notification.announcement.dto.AnnouncementCreateDTO;
import com.yigongbao.module.notification.announcement.dto.AnnouncementPageDTO;
import com.yigongbao.module.notification.announcement.dto.AnnouncementRecipientPageDTO;
import com.yigongbao.module.notification.announcement.dto.AnnouncementTargetDTO;
import com.yigongbao.module.notification.announcement.entity.SystemAnnouncementEntity;
import com.yigongbao.module.notification.announcement.entity.SystemAnnouncementAuditLogEntity;
import com.yigongbao.module.notification.announcement.service.ISystemAnnouncementService;
import com.yigongbao.module.notification.announcement.vo.AnnouncementPendingVO;
import com.yigongbao.module.notification.announcement.vo.AnnouncementPreviewVO;
import com.yigongbao.module.notification.announcement.vo.AnnouncementRecipientVO;
import com.yigongbao.module.notification.announcement.vo.AnnouncementStatisticsVO;
import com.yigongbao.module.notification.announcement.vo.AnnouncementDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 系统公告管理与用户确认接口。 */
@Tag(name = "系统公告", description = "系统公告草稿、发布、撤回和用户确认")
@RestController
@RequestMapping("/notification/announcements")
@RequiredArgsConstructor
public class SystemAnnouncementController {

    private final ISystemAnnouncementService announcementService;

    @Operation(summary = "分页查询系统公告")
    @PostMapping("/page")
    @RequirePermission("notification:announcement:list")
    public Result<IPage<SystemAnnouncementEntity>> page(@RequestBody AnnouncementPageDTO query) {
        return Result.success(announcementService.page(query));
    }

    @Operation(summary = "查询系统公告详情")
    @GetMapping("/{id}")
    @RequirePermission("notification:announcement:view")
    public Result<AnnouncementDetailVO> get(@PathVariable Long id) {
        return Result.success(announcementService.get(id));
    }

    @Operation(summary = "创建系统公告草稿")
    @PostMapping
    @RequirePermission("notification:announcement:create")
    public Result<Long> create(@Valid @RequestBody AnnouncementCreateDTO dto) {
        return Result.success(announcementService.createDraft(dto, StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "更新系统公告草稿")
    @PutMapping("/{id}")
    @RequirePermission("notification:announcement:update")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AnnouncementCreateDTO dto) {
        announcementService.updateDraft(id, dto, StpUtil.getLoginIdAsLong());
        return Result.success();
    }

    @Operation(summary = "预览系统公告")
    @PostMapping("/preview")
    @RequirePermission("notification:announcement:preview")
    public Result<AnnouncementPreviewVO> preview(@Valid @RequestBody AnnouncementCreateDTO dto) {
        return Result.success(announcementService.preview(dto));
    }

    @Operation(summary = "预估公告目标人数")
    @PostMapping("/estimate-target")
    @RequirePermission("notification:announcement:preview")
    public Result<Integer> estimateTarget(@Valid @RequestBody AnnouncementTargetDTO target) {
        return Result.success(announcementService.estimateTarget(target));
    }

    @Operation(summary = "立即发布系统公告")
    @PostMapping("/{id}/publish")
    @RequirePermission("notification:announcement:publish")
    public Result<Void> publish(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        announcementService.publish(id, StpUtil.getLoginIdAsLong(), request.getRemoteAddr());
        return Result.success();
    }

    @Operation(summary = "撤回系统公告")
    @PostMapping("/{id}/revoke")
    @RequirePermission("notification:announcement:revoke")
    public Result<Void> revoke(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        announcementService.revoke(id, StpUtil.getLoginIdAsLong(), request.getRemoteAddr());
        return Result.success();
    }

    @Operation(summary = "查询公告确认统计")
    @GetMapping("/{id}/statistics")
    @RequirePermission("notification:announcement:statistics")
    public Result<AnnouncementStatisticsVO> statistics(@PathVariable Long id) {
        return Result.success(announcementService.statistics(id));
    }

    @Operation(summary = "分页查询公告接收人明细")
    @PostMapping("/{id}/recipients/page")
    @RequirePermission("notification:announcement:statistics")
    public Result<IPage<AnnouncementRecipientVO>> recipients(@PathVariable Long id,
                                                               @RequestBody AnnouncementRecipientPageDTO query) {
        return Result.success(announcementService.recipients(id, query));
    }

    @Operation(summary = "查询公告发布撤回日志")
    @GetMapping("/{id}/audit-logs")
    @RequirePermission("notification:announcement:view")
    public Result<IPage<SystemAnnouncementAuditLogEntity>> auditLogs(@PathVariable Long id,
                                                                       @RequestParam(required = false) Integer pageNum,
                                                                       @RequestParam(required = false) Integer pageSize) {
        return Result.success(announcementService.auditLogs(id, pageNum, pageSize));
    }

    @Operation(summary = "查询当前用户待确认公告")
    @GetMapping("/pending")
    public Result<List<AnnouncementPendingVO>> pending() {
        return Result.success(announcementService.listPending(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "查询当前用户公告详情")
    @GetMapping("/pending/{id}")
    public Result<AnnouncementPendingVO> pendingDetail(@PathVariable Long id) {
        return Result.success(announcementService.getPending(id, StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "确认系统公告")
    @PutMapping("/{id}/acknowledge")
    public Result<Void> acknowledge(@PathVariable Long id) {
        announcementService.acknowledge(id, StpUtil.getLoginIdAsLong());
        return Result.success();
    }
}
