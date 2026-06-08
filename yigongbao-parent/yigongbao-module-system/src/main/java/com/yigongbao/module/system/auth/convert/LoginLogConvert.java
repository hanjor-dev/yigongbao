package com.yigongbao.module.system.auth.convert;

import com.yigongbao.module.system.auth.entity.LoginLogEntity;
import com.yigongbao.module.system.auth.vo.LoginLogVO;
import org.springframework.beans.BeanUtils;

/**
 * 登录日志转换器
 *
 * @author hanjor
 * @date 2026-06-08
 */
public class LoginLogConvert {

    private LoginLogConvert() {
    }

    public static LoginLogVO toVO(LoginLogEntity entity) {
        if (entity == null) {
            return null;
        }
        LoginLogVO vo = new LoginLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
