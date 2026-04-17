package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.dto.ReviewPassDTO;
import com.yigongbao.module.design.dto.ReviewRejectDTO;
import com.yigongbao.module.design.entity.DesignReviewEntity;
import com.yigongbao.module.design.mapper.DesignReviewMapper;
import com.yigongbao.module.design.service.DesignWorkorderService;
import com.yigongbao.module.design.vo.DesignReviewDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderDetailVO;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DesignReviewServiceImpl 单元测试
 *
 * @author hanjor
 * @date 2026-04-17
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DesignReviewServiceImplTest {

    @Mock private DesignReviewMapper designReviewMapper;
    @Mock private OrderMainService orderMainService;
    @Mock private UserService userService;
    @Mock private DesignWorkorderService designWorkorderService;
    @Mock private FlowFacade flowFacade;

    @InjectMocks
    private DesignReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() throws Exception {
        // 反射注入 baseMapper（继承 ServiceImpl 时必须）
        Field baseMapperField = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class
                .getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(reviewService, designReviewMapper);
    }

    // ==================== listReviewWorkorders ====================

    @Nested
    class ListReviewWorkorders {

        @Test
        void forcesStatusTo2040() {
            // 前端传入错误的 status，应被强制覆盖为 2040
            DesignWorkorderQueryDTO queryDTO = new DesignWorkorderQueryDTO();
            queryDTO.setStatus(2020);
            when(designWorkorderService.listWorkorders(any())).thenReturn(new Page<>());

            reviewService.listReviewWorkorders(queryDTO);

            assertEquals(FlowStatusEnum.DESIGN_REVIEWING.getValue(), queryDTO.getStatus());
            verify(designWorkorderService).listWorkorders(queryDTO);
        }
    }

    // ==================== getReviewDetail ====================

    @Nested
    class GetReviewDetail {

        @Test
        void success_withReviewHistory() {
            DesignWorkorderDetailVO workorderDetail = new DesignWorkorderDetailVO();
            workorderDetail.setId(1L);
            workorderDetail.setOrderCode("ORD-001");
            when(designWorkorderService.getWorkorderDetail(1L)).thenReturn(workorderDetail);

            DesignReviewEntity review = new DesignReviewEntity();
            review.setId(10L);
            review.setOrderId(1L);
            review.setReviewerName("审核员A");
            review.setReviewResult(0); // 驳回
            review.setRejectReason("图纸不完整");
            review.setReviewTime(LocalDateTime.now());
            when(designReviewMapper.selectList(any())).thenReturn(List.of(review));

            DesignReviewDetailVO detail = reviewService.getReviewDetail(1L);

            assertNotNull(detail);
            assertEquals("ORD-001", detail.getOrderCode());
            assertEquals(1, detail.getReviewHistory().size());
            assertEquals("驳回", detail.getReviewHistory().get(0).getReviewResultName());
            assertEquals("图纸不完整", detail.getReviewHistory().get(0).getRejectReason());
        }

        @Test
        void success_passResultName() {
            DesignWorkorderDetailVO workorderDetail = new DesignWorkorderDetailVO();
            workorderDetail.setId(1L);
            when(designWorkorderService.getWorkorderDetail(1L)).thenReturn(workorderDetail);

            DesignReviewEntity review = new DesignReviewEntity();
            review.setId(11L);
            review.setReviewResult(1); // 通过
            review.setReviewTime(LocalDateTime.now());
            when(designReviewMapper.selectList(any())).thenReturn(List.of(review));

            DesignReviewDetailVO detail = reviewService.getReviewDetail(1L);
            assertEquals("通过", detail.getReviewHistory().get(0).getReviewResultName());
        }

        @Test
        void success_emptyHistory() {
            DesignWorkorderDetailVO workorderDetail = new DesignWorkorderDetailVO();
            workorderDetail.setId(1L);
            when(designWorkorderService.getWorkorderDetail(1L)).thenReturn(workorderDetail);
            when(designReviewMapper.selectList(any())).thenReturn(Collections.emptyList());

            DesignReviewDetailVO detail = reviewService.getReviewDetail(1L);
            assertTrue(detail.getReviewHistory().isEmpty());
        }
    }

    // ==================== reviewPass ====================

    @Nested
    class ReviewPass {

        @Test
        void success_needsPhysicalDelivery() {
            OrderMainEntity order = buildOrder(FlowStatusEnum.DESIGN_REVIEWING.getValue(), 1);
            when(orderMainService.getById(1L)).thenReturn(order);
            setupReviewer(100L, "审核员A");
            when(designReviewMapper.insert(any(DesignReviewEntity.class))).thenReturn(1);

            // flow 模块内部自动跳转到 3010（needsPhysicalDelivery=1）
            TransitionResult result = TransitionResult.ofWithPhaseChange(30, 2050, 3010);
            when(flowFacade.executeFlow(eq(1L), eq(FlowActionEnum.DESIGN_REVIEW_PASS), any()))
                    .thenReturn(result);
            when(orderMainService.updateById(any())).thenReturn(true);

            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

                ReviewPassDTO dto = new ReviewPassDTO();
                dto.setComment("设计合格");

                assertDoesNotThrow(() -> reviewService.reviewPass(1L, dto));
                // 验证写入了审核通过记录
                verify(designReviewMapper).insert(argThat((DesignReviewEntity r) -> Integer.valueOf(1).equals(r.getReviewResult())));
                // flow 只调用一次
                verify(flowFacade, times(1)).executeFlow(eq(1L), eq(FlowActionEnum.DESIGN_REVIEW_PASS), any());
                // 落库状态为最终可见状态 3010
                verify(orderMainService).updateById(argThat(u -> Integer.valueOf(3010).equals(u.getStatus())));
            }
        }

        @Test
        void success_noPhysicalDelivery() {
            OrderMainEntity order = buildOrder(FlowStatusEnum.DESIGN_REVIEWING.getValue(), 0);
            when(orderMainService.getById(1L)).thenReturn(order);
            setupReviewer(100L, "审核员A");
            when(designReviewMapper.insert(any(DesignReviewEntity.class))).thenReturn(1);

            // flow 自动跳转到 7010（needsPhysicalDelivery=0）
            TransitionResult result = TransitionResult.ofWithPhaseChange(70, 2050, 7010);
            when(flowFacade.executeFlow(eq(1L), eq(FlowActionEnum.DESIGN_REVIEW_PASS), any()))
                    .thenReturn(result);
            when(orderMainService.updateById(any())).thenReturn(true);

            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                assertDoesNotThrow(() -> reviewService.reviewPass(1L, null));
                verify(orderMainService).updateById(argThat(u -> Integer.valueOf(7010).equals(u.getStatus())));
            }
        }

        @Test
        void orderNotFound() {
            when(orderMainService.getById(999L)).thenReturn(null);
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                assertThrows(BusinessException.class, () -> reviewService.reviewPass(999L, null));
            }
        }

        @Test
        void wrongStatus() {
            OrderMainEntity order = buildOrder(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue(), 1);
            when(orderMainService.getById(1L)).thenReturn(order);
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                assertThrows(BusinessException.class, () -> reviewService.reviewPass(1L, null));
            }
        }
    }

    // ==================== reviewReject ====================

    @Nested
    class ReviewReject {

        @Test
        void success() {
            OrderMainEntity order = buildOrder(FlowStatusEnum.DESIGN_REVIEWING.getValue(), 1);
            order.setDesignerId(200L);
            order.setDesignerName("设计师A");
            when(orderMainService.getById(1L)).thenReturn(order);
            setupReviewer(100L, "审核员A");
            when(designReviewMapper.insert(any(DesignReviewEntity.class))).thenReturn(1);

            TransitionResult result = TransitionResult.of(20, FlowStatusEnum.DESIGN_REVIEW_REJECTED.getValue());
            when(flowFacade.executeFlow(eq(1L), eq(FlowActionEnum.DESIGN_REVIEW_REJECT), any()))
                    .thenReturn(result);
            when(orderMainService.updateById(any())).thenReturn(true);

            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

                ReviewRejectDTO dto = new ReviewRejectDTO();
                dto.setRejectReason("图纸格位不清晰");

                assertDoesNotThrow(() -> reviewService.reviewReject(1L, dto));
                // 验证写入驳回记录
                verify(designReviewMapper).insert(argThat((DesignReviewEntity r) ->
                        Integer.valueOf(0).equals(r.getReviewResult())
                                && "图纸格位不清晰".equals(r.getRejectReason())));
                // 验证驳回原因写入 order_main.design_review_remark
                verify(orderMainService).updateById(argThat(u ->
                        "图纸格位不清晰".equals(u.getDesignReviewRemark())));
            }
        }

        @Test
        void orderNotFound() {
            when(orderMainService.getById(999L)).thenReturn(null);
            ReviewRejectDTO dto = new ReviewRejectDTO();
            dto.setRejectReason("原因");
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                assertThrows(BusinessException.class, () -> reviewService.reviewReject(999L, dto));
            }
        }

        @Test
        void wrongStatus() {
            OrderMainEntity order = buildOrder(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue(), 1);
            when(orderMainService.getById(1L)).thenReturn(order);
            ReviewRejectDTO dto = new ReviewRejectDTO();
            dto.setRejectReason("原因");
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                assertThrows(BusinessException.class, () -> reviewService.reviewReject(1L, dto));
            }
        }

        @Test
        void designerNotAssigned() {
            // 设计师未分配（designerId 为 null），驳回应抛异常
            OrderMainEntity order = buildOrder(FlowStatusEnum.DESIGN_REVIEWING.getValue(), 1);
            // 不设置 designerId，默认为 null
            when(orderMainService.getById(1L)).thenReturn(order);
            ReviewRejectDTO dto = new ReviewRejectDTO();
            dto.setRejectReason("原因");
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
                assertThrows(BusinessException.class, () -> reviewService.reviewReject(1L, dto));
            }
        }
    }

    // ==================== 辅助方法 ====================

    private OrderMainEntity buildOrder(int status, int needsPhysicalDelivery) {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(1L);
        order.setStatus(status);
        order.setPhase(20);
        order.setNeedsPhysicalDelivery(needsPhysicalDelivery);
        return order;
    }

    private void setupReviewer(Long userId, String name) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setRealName(name);
        when(userService.getById(userId)).thenReturn(user);
    }
}
