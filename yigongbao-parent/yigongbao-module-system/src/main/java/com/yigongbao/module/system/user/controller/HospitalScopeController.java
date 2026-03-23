package com.yigongbao.module.system.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.hospital.vo.HospitalVO;
import com.yigongbao.module.system.role.entity.RoleEntity;
import com.yigongbao.module.system.role.service.RoleService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserHospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 医院权限范围 Controller
 * 处理需要根据用户权限过滤的医院查询，用于业务员创建订单等业务场景
 *
 * @author hanjor
 * @date 2026-03-20
 */
@RestController
@RequestMapping("/api/system/hospital-scope")
@RequiredArgsConstructor
public class HospitalScopeController {

    private final UserMapper userMapper;
    private final RoleService roleService;
    private final UserHospitalService userHospitalService;

    /**
     * 获取当前用户可操作的医院列表（根据用户权限过滤）
     * 用于业务员创建订单等业务场景时的医院选择
     *
     * 过滤逻辑：
     * 1. 获取用户关联的角色
     * 2. 检查角色的 hospitalScopeEnabled 字段
     *    - hospitalScopeEnabled == 1：查询 sys_user_hospital 关联表，返回用户可操作的医院列表
     *    - hospitalScopeEnabled == 0 或 无角色：返回空列表（无权查看任何医院）
     *
     * @param userId 用户ID
     * @return 当前用户可操作的医院列表
     */
    @GetMapping("/my-hospitals/{userId}")
    public Result<List<HospitalVO>> getMyHospitals(@PathVariable Long userId) {
        // 1. 获取用户信息和角色
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            return Result.success(new ArrayList<>());
        }

        // 2. 检查角色的 hospitalScopeEnabled
        boolean hospitalScopeEnabled = false;
        if (user.getRoleId() != null) {
            RoleEntity role = roleService.getById(user.getRoleId());
            if (role != null && role.getHospitalScopeEnabled() != null
                    && role.getHospitalScopeEnabled() == StatusConstants.YES) {
                hospitalScopeEnabled = true;
            }
        }

        // 3. 根据 hospitalScopeEnabled 决定查询范围
        List<HospitalVO> result;
        if (hospitalScopeEnabled) {
            result = userHospitalService.getHospitalsByUserId(userId);
        } else {
            result = new ArrayList<>();
        }

        return Result.success(result);
    }

    /**
     * 获取当前登录用户可操作的医院列表（从请求上下文获取用户ID）
     * 用于已登录用户直接获取自己的医院权限范围
     *
     * @return 当前用户可操作的医院列表
     */
    @GetMapping("/my-hospitals")
    public Result<List<HospitalVO>> getMyHospitalsByLogin() {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return getMyHospitals(currentUserId);
    }
}
