package com.yigongbao.module.order.validator;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.basic.bodyPart.service.BodyPartService;
import com.yigongbao.module.basic.bodyPart.vo.BodyPartDetailVO;
import com.yigongbao.module.basic.hospitalDept.service.HospitalDeptService;
import com.yigongbao.module.basic.rebuildProject.service.RebuildProjectService;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectDetailVO;
import com.yigongbao.module.order.entity.OrderItemDraftEntity;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.doctor.service.DoctorService;
import com.yigongbao.module.system.doctor.dto.QuickAddDoctorDTO;
import com.yigongbao.module.system.doctor.dto.UpdateDoctorDTO;
import com.yigongbao.module.system.doctor.vo.DoctorVO;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class OrderDataValidatorTest {

    @Mock
    private OrgService orgService;
    @Mock
    private UserService userService;
    @Mock
    private DoctorService doctorService;
    @Mock
    private BodyPartService bodyPartService;
    @Mock
    private RebuildProjectService rebuildProjectService;
    @Mock
    private UserHospitalService userHospitalService;
    @Mock
    private ConfigService configService;
    @Mock
    private HospitalDeptService hospitalDeptService;

    @InjectMocks
    private OrderDataValidator validator;

    @BeforeEach
    void setUp() {
        BodyPartDetailVO bodyPart = new BodyPartDetailVO();
        bodyPart.setName("头部");
        lenient().when(bodyPartService.getDetailById(1L)).thenReturn(bodyPart);
    }

    @Test
    void fillsMissingProjectCategoryNameFromDictionary() {
        RebuildProjectDetailVO project = project("13.2", null);
        when(rebuildProjectService.getDetailById(196L)).thenReturn(project);
        when(rebuildProjectService.getCategoryNameByCode("13.2")).thenReturn("导板");

        OrderItemEntity item = item(196L, "前端伪造名称");

        validator.validateAndFillItemsForOrder(List.of(item), OrderDataValidator.ValidateMode.DIRECT);

        assertEquals("导板", item.getCategoryName());
        assertEquals("13.2", item.getCategoryCode());
    }

    @Test
    void retainsProjectCategoryNameAndIgnoresFrontendName() {
        when(rebuildProjectService.getDetailById(196L)).thenReturn(project("13.2", "导板"));
        when(rebuildProjectService.getCategoryNameByCode("13.2")).thenReturn("导板");

        OrderItemEntity item = item(196L, "前端伪造名称");

        validator.validateAndFillItemsForOrder(List.of(item), OrderDataValidator.ValidateMode.DIRECT);

        assertEquals("导板", item.getCategoryName());
    }

    @Test
    void validatesCategoryCodeEvenWhenProjectNameExists() {
        when(rebuildProjectService.getDetailById(196L)).thenReturn(project("13.2", "导板"));
        when(rebuildProjectService.getCategoryNameByCode("13.2")).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> validator.validateAndFillItemsForOrder(
                        List.of(item(196L, null)), OrderDataValidator.ValidateMode.DIRECT));
    }

    @Test
    void fillsMissingProjectCategoryNameWhenSubmittingDraft() {
        when(rebuildProjectService.getDetailById(196L)).thenReturn(project("13.2", null));
        when(rebuildProjectService.getCategoryNameByCode("13.2")).thenReturn("导板");

        OrderItemEntity item = item(196L, null);

        validator.validateAndFillItemsForOrder(List.of(item), OrderDataValidator.ValidateMode.SUBMIT);

        assertEquals("导板", item.getCategoryName());
    }

    @Test
    void rejectsMissingCategoryCodeForFormalOrder() {
        when(rebuildProjectService.getDetailById(196L)).thenReturn(project(null, null));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> validator.validateAndFillItemsForOrder(
                        List.of(item(196L, null)), OrderDataValidator.ValidateMode.DIRECT));

        assertEquals(ErrorCodeEnum.INVALID_PARAMETER.getCode(), exception.getCode());
    }

    @Test
    void rejectsWhenDictionaryCannotProvideCategoryName() {
        when(rebuildProjectService.getDetailById(196L)).thenReturn(project("13.2", null));
        when(rebuildProjectService.getCategoryNameByCode("13.2")).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> validator.validateAndFillItemsForOrder(
                        List.of(item(196L, null)), OrderDataValidator.ValidateMode.DIRECT));
    }

    @Test
    void rejectsWhenDictionaryLookupFails() {
        when(rebuildProjectService.getDetailById(196L)).thenReturn(project("13.2", null));
        when(rebuildProjectService.getCategoryNameByCode("13.2"))
                .thenThrow(new IllegalStateException("dictionary unavailable"));

        assertThrows(BusinessException.class,
                () -> validator.validateAndFillItemsForOrder(
                        List.of(item(196L, null)), OrderDataValidator.ValidateMode.DIRECT));
    }

    @Test
    void draftValidationStillAllowsMissingProjectCategoryCode() {
        OrderItemDraftEntity item = new OrderItemDraftEntity();

        assertDoesNotThrow(() -> validator.validateAndFillItems(List.of(item), OrderDataValidator.ValidateMode.DRAFT));
    }

    @Test
    void draftValidationStillAllowsExistingProjectWithMissingCategoryData() {
        when(rebuildProjectService.getDetailById(196L)).thenReturn(project(null, null));
        OrderItemDraftEntity item = new OrderItemDraftEntity();
        item.setProjectId(196L);

        assertDoesNotThrow(() -> validator.validateAndFillItems(List.of(item), OrderDataValidator.ValidateMode.DRAFT));
    }

    @Test
    void modifyResolvesDoctorByHospitalAndNameWhenSubmittedIdIsStale() {
        DoctorVO resolved = new DoctorVO();
        resolved.setId(76L);
        resolved.setDoctorName("罗政强");
        resolved.setDoctorPhone("18674131702");
        resolved.setHospitalId(189L);
        resolved.setStatus(1);
        when(doctorService.quickAdd(any(QuickAddDoctorDTO.class))).thenReturn(resolved);

        OrderMainEntity order = new OrderMainEntity();
        order.setHospitalId(189L);
        order.setDoctorId(38L);
        order.setDoctorName("郭风劲");

        validator.validateAndFillForModify(order, null, 38L,
                "罗政强", "18674131702");

        assertEquals(76L, order.getDoctorId());
        assertEquals("罗政强", order.getDoctorName());
        assertEquals("18674131702", order.getDoctorPhone());
        verify(doctorService, never()).update(anyLong(), any(UpdateDoctorDTO.class));
    }

    @Test
    void modifyClearsDoctorWhenDoctorFieldsAreCleared() {
        OrderMainEntity order = new OrderMainEntity();
        order.setHospitalId(189L);
        order.setDoctorId(38L);
        order.setDoctorName("郭风劲");
        order.setDoctorPhone("18674131702");

        validator.validateAndFillForModify(order, null, 38L, "", "");

        assertNull(order.getDoctorId());
        assertNull(order.getDoctorName());
        assertNull(order.getDoctorPhone());
    }

    private static OrderItemEntity item(Long projectId, String categoryName) {
        OrderItemEntity item = new OrderItemEntity();
        item.setBodyPartId(1L);
        item.setProjectId(projectId);
        item.setCategoryName(categoryName);
        return item;
    }

    private static RebuildProjectDetailVO project(String categoryCode, String categoryName) {
        RebuildProjectDetailVO project = new RebuildProjectDetailVO();
        project.setId(196L);
        project.setName("来图打印");
        project.setStatus(1);
        project.setCategoryCode(categoryCode);
        project.setCategoryName(categoryName);
        return project;
    }
}
