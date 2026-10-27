package com.yigongbao.module.backup.security.verifier;

/** 机器身份验证统一失败；不向调用方暴露具体失败细节。 */
public class MachineVerificationException extends RuntimeException {

    public MachineVerificationException() {
        super("verification rejected");
    }
}
