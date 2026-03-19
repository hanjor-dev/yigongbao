package com.yigongbao.module.system.auth.vo;

import com.yigongbao.module.system.resource.vo.ResourceVO;
import com.yigongbao.module.system.user.vo.UserVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 登录响应 VO
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class LoginVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Token
     */
    private String token;

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 菜单树（用于前端渲染侧边栏）
     */
    private List<ResourceVO> menus;

    /**
     * 权限列表（用于按钮级权限控制）
     */
    private List<String> permissions;
}
