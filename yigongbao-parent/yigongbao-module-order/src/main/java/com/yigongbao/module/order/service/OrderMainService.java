package com.yigongbao.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.order.dto.order.AuditOrderDTO;
import com.yigongbao.module.order.dto.order.CreateOrderDTO;
import com.yigongbao.module.order.dto.order.OrderPageDTO;
import com.yigongbao.module.order.dto.order.UpdateOrderDTO;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.order.vo.order.OrderColumnConfigVO;
import com.yigongbao.module.order.vo.order.OrderDetailVO;
import com.yigongbao.module.order.vo.order.OrderListVO;

import java.util.List;

/**
 * 订单主表 Service
 *
 * @author hanjor
 * @date 2026-03-31
 */
public interface OrderMainService extends IService<OrderMainEntity> {

    /**
     * 分页查询订单列表
     *
     * @param dto 查询参数
     * @return 订单列表
     */
    IPage<OrderListVO> listOrders(OrderPageDTO dto);

    /**
     * 查询订单详情
     *
     * @param id 订单ID
     * @return 订单详情
     */
    OrderDetailVO getOrderDetail(Long id);

    /**
     * 更新订单信息（公司管理员/10分钟内）
     *
     * @param id 订单ID
     * @param dto 更新参数
     */
    void updateOrder(Long id, UpdateOrderDTO dto);

    /**
     * 删除订单（仅草稿状态）
     *
     * @param id 订单ID
     */
    void removeOrder(Long id);

    /**
     * 校验订单是否为经典案例，如果是则抛出异常
     *
     * @param orderId 订单ID
     * @param operation 操作描述（用于日志）
     * @throws BusinessException 订单为经典案例时抛出CLASSIC_CASE_PROTECTED
     */
    void checkNotClassicCase(Long orderId, String operation);

    /**
     * 提交订单（提交审核）
     *
     * @param id 订单ID
     */
    void submitOrder(Long id);

    /**
     * 撤回订单
     *
     * @param id 订单ID
     */
    void withdrawOrder(Long id);

    /**
     * 审核通过
     *
     * @param id 订单ID
     * @param dto 审核参数
     */
    void auditPass(Long id, AuditOrderDTO dto);

    /**
     * 审核驳回
     *
     * @param id 订单ID
     * @param dto 审核参数
     */
    void auditReject(Long id, AuditOrderDTO dto);

    /**
     * 取消订单（全阶段可用）
     *
     * @param id 订单ID
     */
    void cancelOrder(Long id);

    /**
     * 查询可执行的动作
     *
     * @param id 订单ID
     * @return 可执行的动作列表
     */
    List<String> listAvailableActions(Long id);

    /**
     * 从草稿创建正式订单
     *
     * @param draft 草稿实体
     * @return 订单ID
     */
    Long createFromDraft(OrderDraftEntity draft);

    /**
     * 直接创建正式订单（直提流程，不经过草稿）
     * 业务员直接填写完整信息后提交订单
     *
     * @param dto 创建订单参数
     * @return 订单ID
     */
    Long createOrder(CreateOrderDTO dto);

    /**
     * 获取当前用户的订单列配置（个人配置 > 系统默认）
     *
     * @return 列配置 VO，均未配置时返回 null
     */
    OrderColumnConfigVO getColumnConfig();

    /**
     * 保存当前用户的订单列配置
     *
     * @param config 列配置 VO
     */
    void saveColumnConfig(OrderColumnConfigVO config);

    /**
     * 重置当前用户列配置（删除个人配置，恢复系统默认）
     */
    void resetColumnConfig();

    /**
     * 手动完成订单（仅限不需要实体交付的订单）
     * 前置条件：订单状态为设计完成(2030)，needsPhysicalDelivery=0
     *
     * @param orderId 订单ID
     * @throws BusinessException 订单不存在、状态错误或需要实体交付
     */
    void manualCompleteOrder(Long orderId);
}
