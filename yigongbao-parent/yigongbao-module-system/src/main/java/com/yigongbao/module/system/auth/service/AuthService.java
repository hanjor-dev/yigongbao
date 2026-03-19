package com.yigongbao.module.system.auth.service;

import com.yigongbao.module.system.auth.dto.ChangePasswordDTO;
import com.yigongbao.module.system.auth.dto.LoginDTO;
import com.yigongbao.module.system.auth.vo.LoginVO;

/**
 * 认证 Service
 *
 * @author hanjor
 * @date 2026-03-19
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param dto 登录参数
     * @return 登录结果
     */
    LoginVO login(LoginDTO dto);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 获取当前用户信息（含菜单、权限）
     *
     * @return 当前用户信息
     */
    LoginVO getCurrentUserInfo();

    /**
     * 修改密码
     *
     * @param userId 用户ID
     * @param dto    修改密码参数
     */
    void changePassword(Long userId, ChangePasswordDTO dto);
}
