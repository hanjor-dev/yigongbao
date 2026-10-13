package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.order.mapper.OrderDraftMapper;
import com.yigongbao.module.order.mapper.OrderItemDraftMapper;
import com.yigongbao.module.order.dto.draft.CreateOrderDraftDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.module.order.service.OrderDraftFileService;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.validator.OrderDataValidator;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.service.UserService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderDraftServiceImplPermissionTest {

    @Mock private OrderDraftMapper draftMapper;
    @Mock private OrderItemDraftMapper itemMapper;
    @Mock private FileService fileService;
    @Mock private OrderMainService orderMainService;
    @Mock private ConfigService configService;
    @Mock private OrderDataValidator validator;
    @Mock private UserService userService;
    @Mock private OrderDraftFileService draftFileService;

    @Spy
    @InjectMocks
    private OrderDraftServiceImpl service;

    @Test
    void validateDraftOwner_rejectsMissingDraft() {
        doReturn(null).when(service).getById(8L);

        var exception = assertThrows(com.yigongbao.common.exception.BusinessException.class,
                () -> service.validateDraftOwner(8L, 1L));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.ORDER_DRAFT_NOT_FOUND.getCode());
    }

    @Test
    void validateDraftOwner_rejectsDraftOwnedByAnotherUser() {
        OrderDraftEntity draft = new OrderDraftEntity();
        draft.setOperatorId(2L);
        doReturn(draft).when(service).getById(8L);

        var exception = assertThrows(com.yigongbao.common.exception.BusinessException.class,
                () -> service.validateDraftOwner(8L, 1L));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.ORDER_DRAFT_NOT_MINE.getCode());
    }

    @Test
    void removeDraft_rejectsSubmittedDraftBeforeDeletingChildren() {
        OrderDraftEntity draft = new OrderDraftEntity();
        draft.setOperatorId(1L);
        draft.setStatus(2);
        doReturn(draft).when(service).getById(8L);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            var exception = assertThrows(com.yigongbao.common.exception.BusinessException.class,
                    () -> service.removeDraft(8L));
            assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.ORDER_DRAFT_ALREADY_SUBMITTED.getCode());
        }
    }

    @Test
    void listDrafts_returnsEmptyPageWhenNotLoggedIn() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenThrow(new RuntimeException("not logged in"));

            IPage<?> page = service.listDrafts(new com.yigongbao.module.order.dto.draft.OrderDraftPageQueryDTO());

            assertThat(page.getTotal()).isZero();
            assertThat(page.getRecords()).isEmpty();
            verifyNoInteractions(draftMapper, itemMapper);
        }
    }

    @Test
    void getDraftDetail_missingDraftThrowsBusinessError() {
        doReturn(null).when(service).getById(12L);

        var exception = assertThrows(com.yigongbao.common.exception.BusinessException.class,
                () -> service.getDraftDetail(12L));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.ORDER_DRAFT_NOT_FOUND.getCode());
    }

    @Test
    void getDraftDetail_rebuildsEmptyItemsAndFileLists() {
        OrderDraftEntity draft = new OrderDraftEntity();
        draft.setId(13L);
        draft.setOperatorId(1L);
        draft.setStatus(1);
        doReturn(draft).when(service).getById(13L);
        when(itemMapper.selectList(any())).thenReturn(java.util.List.of());
        when(draftFileService.listByDraftId(13L)).thenReturn(java.util.List.of());

        var detail = service.getDraftDetail(13L);

        assertThat(detail.getId()).isEqualTo(13L);
        assertThat(detail.getItemCount()).isZero();
        assertThat(detail.getImageDataFiles()).isEmpty();
        assertThat(detail.getImageReportFiles()).isEmpty();
        assertThat(detail.getApprovalFiles()).isEmpty();
    }

    @Test
    void saveDraft_rejectsInvalidBusinessTypeBeforePersistence() {
        CreateOrderDraftDTO dto = new CreateOrderDraftDTO();
        dto.setBusinessType("invalid");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            var exception = assertThrows(com.yigongbao.common.exception.BusinessException.class,
                    () -> service.saveDraft(dto));
            assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.ORDER_BUSINESS_TYPE_INVALID.getCode());
            verify(service, never()).saveOrUpdate(any());
        }
    }

    @Test
    void saveDraft_newDraftUsesCurrentUserAndPersistsEditableFields() {
        CreateOrderDraftDTO dto = new CreateOrderDraftDTO();
        dto.setOrderType(1);
        dto.setNeedsPhysicalDelivery(0);
        dto.setBusinessType("11.1");
        dto.setOrgId(2L);
        dto.setHospitalId(3L);
        dto.setPatientName("患者");
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setRealName("操作员");
        user.setPhone("13800000000");
        when(userService.getById(1L)).thenReturn(user);
        when(configService.getConfigValue(any())).thenReturn(null);
        doAnswer(invocation -> {
            OrderDraftEntity entity = invocation.getArgument(0);
            entity.setId(14L);
            return true;
        }).when(service).saveOrUpdate(any(OrderDraftEntity.class));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertThat(service.saveDraft(dto)).isEqualTo(14L);
        }

        verify(validator).validateAndFillMaster(any(), eq(2L), eq(3L),
                isNull(), isNull(), isNull(), isNull(), eq(1L),
                eq(com.yigongbao.module.order.validator.OrderDataValidator.ValidateMode.DRAFT));
    }
}
