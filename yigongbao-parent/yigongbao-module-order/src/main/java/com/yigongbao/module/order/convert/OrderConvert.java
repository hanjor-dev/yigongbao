package com.yigongbao.module.order.convert;

import cn.hutool.core.util.StrUtil;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.order.vo.AuditInfo;
import com.yigongbao.module.order.vo.order.OrderDetailVO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 订单转换器
 * 处理订单审核信息的转换逻辑
 *
 * @author hanjor
 * @date 2026-06-04
 */
@Component
@RequiredArgsConstructor
public class OrderConvert {

    private final UserService userService;

    /**
     * 填充订单VO的审核信息
     *
     * @param entity 订单实体
     * @param vo     订单VO
     */
    public void fillAuditInfo(OrderMainEntity entity, OrderDetailVO vo) {
        // 计算审核进度和阶段
        vo.setAuditProgress(calculateAuditProgress(entity));
        vo.setAuditStage(calculateAuditStage(entity));

        // 构建区域审核信息（仅试用订单）
        if ("11.3".equals(entity.getBusinessType()) && entity.getRegionalAuditStatus() != null) {
            vo.setRegionalAudit(buildAuditInfo(
                entity.getRegionalAuditStatus(),
                entity.getRegionalAuditBy(),
                entity.getRegionalAuditTime(),
                entity.getRegionalAuditRemark()
            ));
        }

        // 构建设计审核信息
        if (entity.getDesignAuditStatus() != null) {
            vo.setDesignAudit(buildAuditInfo(
                entity.getDesignAuditStatus(),
                entity.getDesignAuditBy(),
                entity.getDesignAuditTime(),
                entity.getDesignAuditRemark()
            ));
        }
    }

    /**
     * 计算审核进度描述
     * 根据订单类型和审核状态返回中文描述
     *
     * @param order 订单实体
     * @return 审核进度描述
     */
    private String calculateAuditProgress(OrderMainEntity order) {
        String businessType = order.getBusinessType();

        if ("11.3".equals(businessType)) {
            // 试用订单：两级审核
            Integer regionalStatus = order.getRegionalAuditStatus();
            Integer designStatus = order.getDesignAuditStatus();

            if (regionalStatus == null || regionalStatus == 0) {
                return "等待区域管理员审核";
            } else if (regionalStatus == 2) {
                return "区域管理员驳回";
            } else if (regionalStatus == 1 && designStatus == 0) {
                return "等待设计管理员审核";
            } else if (designStatus == 2) {
                return "设计管理员驳回";
            } else if (designStatus == 1) {
                return "数据审核通过";
            }
        } else {
            // 其他订单：单级审核
            Integer designStatus = order.getDesignAuditStatus();

            if (designStatus == null || designStatus == 0) {
                return "等待设计管理员审核";
            } else if (designStatus == 2) {
                return "设计管理员驳回";
            } else if (designStatus == 1) {
                return "数据审核通过";
            }
        }

        return "未知状态";
    }

    /**
     * 计算当前审核环节
     * 返回枚举标识用于前端判断
     *
     * @param order 订单实体
     * @return 审核环节标识
     */
    private String calculateAuditStage(OrderMainEntity order) {
        String businessType = order.getBusinessType();

        if ("11.3".equals(businessType)) {
            // 试用订单
            Integer regionalStatus = order.getRegionalAuditStatus();
            Integer designStatus = order.getDesignAuditStatus();

            if (regionalStatus != null && regionalStatus == 2) {
                return "REGIONAL_REJECTED";
            } else if (designStatus != null && designStatus == 2) {
                return "DESIGN_REJECTED";
            } else if (regionalStatus == null || regionalStatus == 0) {
                return "REGIONAL_PENDING";
            } else if (regionalStatus == 1 && (designStatus == null || designStatus == 0)) {
                return "DESIGN_PENDING";
            } else if (designStatus == 1) {
                return "PASSED";
            }
        } else {
            // 其他订单
            Integer designStatus = order.getDesignAuditStatus();

            if (designStatus != null && designStatus == 2) {
                return "DESIGN_REJECTED";
            } else if (designStatus == null || designStatus == 0) {
                return "DESIGN_PENDING";
            } else if (designStatus == 1) {
                return "PASSED";
            }
        }

        return "UNKNOWN";
    }

    /**
     * 构建审核信息对象
     * 将审核状态字段转换为AuditInfo
     *
     * @param status     审核状态：0-未审核，1-已通过，2-已驳回
     * @param auditorId  审核人ID
     * @param auditTime  审核时间
     * @param remark     审核备注
     * @return AuditInfo对象
     */
    private AuditInfo buildAuditInfo(Integer status, Long auditorId,
                                      java.time.LocalDateTime auditTime, String remark) {
        AuditInfo info = new AuditInfo();
        info.setStatus(status);
        info.setStatusDesc(getAuditStatusDesc(status));
        info.setAuditorId(auditorId);
        info.setAuditTime(auditTime);
        info.setRemark(remark);

        // 查询审核人姓名
        if (auditorId != null) {
            UserEntity user = userService.getById(auditorId);
            if (user != null) {
                info.setAuditorName(user.getRealName());
            }
        }

        return info;
    }

    /**
     * 转换审核状态为中文描述
     *
     * @param status 审核状态
     * @return 中文描述
     */
    private String getAuditStatusDesc(Integer status) {
        if (status == null) {
            return "待审核";
        }
        switch (status) {
            case 0:
                return "待审核";
            case 1:
                return "已通过";
            case 2:
                return "已驳回";
            default:
                return "未知";
        }
    }
}
