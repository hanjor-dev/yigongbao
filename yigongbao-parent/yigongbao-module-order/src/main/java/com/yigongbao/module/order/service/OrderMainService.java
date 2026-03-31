package com.yigongbao.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.order.dto.order.AuditOrderDTO;
import com.yigongbao.module.order.dto.order.CreateOrderDTO;
import com.yigongbao.module.order.dto.order.UpdateOrderDTO;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.module.order.entity.OrderMainEntity;
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
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param orderCode 订单编号（可选）
     * @param hospitalId 医院ID（可选）
     * @param status 状态（可选）
     * @return 订单列表
     */
    IPage<OrderListVO> listOrders(Integer pageNum, Integer pageSize, String orderCode, Long hospitalId, Integer status);

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
}
