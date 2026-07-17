package com.yigongbao.module.design.service.impl;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.mapper.DesignPackageFileScreenshotMapper;
import com.yigongbao.module.design.service.DesignPackageFileService;
import com.yigongbao.module.design.service.DesignPackageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DesignScreenshotServiceImplTest {

    @Mock private DesignPackageFileService packageFileService;
    @Mock private DesignPackageService packageService;
    @Mock private FileService fileService;
    @Mock private DesignPackageFileScreenshotMapper screenshotMapper;

    @Spy
    @InjectMocks
    private DesignScreenshotServiceImpl service;

    @Test
    void getScreenshot_rejectsMissingPackageFile() {
        when(packageFileService.getById(20L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getScreenshot(10L, 20L));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.DESIGN_PACKAGE_FILE_NOT_FOUND.getCode());
        verifyNoInteractions(fileService);
    }

    @Test
    void getScreenshot_rejectsFileFromAnotherPackage() {
        DesignPackageFileEntity file = new DesignPackageFileEntity();
        file.setId(20L);
        file.setPackageId(99L);
        when(packageFileService.getById(20L)).thenReturn(file);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getScreenshot(10L, 20L));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.DESIGN_PACKAGE_FILE_WRONG_PACKAGE.getCode());
        verifyNoInteractions(fileService);
    }

    @Test
    void listFileIdsByPackageFileIds_returnsEmptyForNullOrEmptyInput() {
        assertThat(service.listFileIdsByPackageFileIds(null)).isEmpty();
        assertThat(service.listFileIdsByPackageFileIds(List.of())).isEmpty();
        verifyNoInteractions(screenshotMapper);
    }

    @Test
    void deleteByPackageFileIds_doesNothingForEmptyInput() {
        service.deleteByPackageFileIds(List.of());

        verifyNoInteractions(screenshotMapper);
    }
}
