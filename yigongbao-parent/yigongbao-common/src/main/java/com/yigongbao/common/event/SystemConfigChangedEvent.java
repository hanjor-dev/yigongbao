package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** 系统配置变更事件，用于通知依赖配置的内存缓存及时失效。 */
@Getter
public class SystemConfigChangedEvent extends ApplicationEvent {

    private final String configKey;

    public SystemConfigChangedEvent(Object source, String configKey) {
        super(source);
        this.configKey = configKey;
    }
}
