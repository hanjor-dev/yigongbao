package com.yigongbao.module.backup.security.verifier;

import jakarta.servlet.http.HttpServletRequest;

/** 内部机器请求验证边界。 */
@FunctionalInterface
public interface MachineRequestVerifier {

    VerifiedMachine verify(HttpServletRequest request, byte[] cachedBody);
}
