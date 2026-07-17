package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.yigongbao.module.design.entity.DesignDrawingEntity;
import com.yigongbao.module.design.entity.DesignInstructionEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.mapper.DesignDrawingMapper;
import com.yigongbao.module.design.mapper.DesignInstructionMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import com.yigongbao.module.design.mapper.DesignProductFileMapper;
import com.yigongbao.module.design.service.impl.DesignProductFileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DesignSimpleServicesTest {

    @BeforeAll
    static void initLambdaCache() {
        Configuration configuration = new Configuration();
        GlobalConfigUtils.getGlobalConfig(configuration);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, DesignDrawingEntity.class);
        TableInfoHelper.initTableInfo(assistant, DesignInstructionEntity.class);
        TableInfoHelper.initTableInfo(assistant, DesignPackageEntity.class);
        TableInfoHelper.initTableInfo(assistant, DesignProductEntity.class);
    }

    @Mock
    private DesignDrawingMapper drawingMapper;
    @Mock
    private DesignInstructionMapper instructionMapper;
    @Mock
    private DesignPackageMapper packageMapper;
    @Mock
    private DesignProductMapper productMapper;
    @Mock
    private DesignProductFileMapper productFileMapper;

    @Spy
    @InjectMocks
    private DesignDrawingServiceImpl drawingService;
    @Spy
    @InjectMocks
    private DesignInstructionServiceImpl instructionService;
    @Spy
    @InjectMocks
    private DesignPackageServiceImpl packageService;
    @InjectMocks
    private SpecReferenceCheckerImpl specReferenceChecker;
    @Spy
    @InjectMocks
    private DesignProductFileServiceImpl productFileService;
    @Mock
    private LambdaQueryChainWrapper<DesignDrawingEntity> drawingQuery;
    @Mock
    private LambdaQueryChainWrapper<DesignInstructionEntity> instructionQuery;

    @BeforeEach
    void injectBaseMappers() {
        ReflectionTestUtils.setField(drawingService, "baseMapper", drawingMapper);
        ReflectionTestUtils.setField(instructionService, "baseMapper", instructionMapper);
        ReflectionTestUtils.setField(packageService, "baseMapper", packageMapper);
        ReflectionTestUtils.setField(productFileService, "baseMapper", productFileMapper);
        doReturn(drawingQuery).when(drawingService).lambdaQuery();
        doReturn(instructionQuery).when(instructionService).lambdaQuery();
        when(drawingQuery.eq(any(), any())).thenReturn(drawingQuery);
        when(drawingQuery.orderByDesc((SFunction<DesignDrawingEntity, ?>) any())).thenReturn(drawingQuery);
        when(drawingQuery.last(any())).thenReturn(drawingQuery);
        when(instructionQuery.eq(any(), any())).thenReturn(instructionQuery);
        when(instructionQuery.orderByDesc((SFunction<DesignInstructionEntity, ?>) any())).thenReturn(instructionQuery);
        when(instructionQuery.last(any())).thenReturn(instructionQuery);
    }

    @Test
    void drawingLatestVersion_returnsHighestVersionFromMapperResults() {
        DesignDrawingEntity latest = new DesignDrawingEntity();
        latest.setId(2L);
        latest.setVersionSeq(2);
        when(drawingQuery.oneOpt()).thenReturn(java.util.Optional.of(latest));

        assertThat(drawingService.getLatestVersion(10L)).isSameAs(latest);
    }

    @Test
    void instructionListVersions_returnsMapperResults() {
        DesignInstructionEntity version = new DesignInstructionEntity();
        version.setPackageId(10L);
        when(instructionQuery.list()).thenReturn(List.of(version));

        assertThat(instructionService.listVersions(10L)).containsExactly(version);
    }

    @Test
    void packageNextSequence_ignoresNullAndReturnsMaxPlusOne() {
        DesignPackageEntity seq2 = new DesignPackageEntity();
        seq2.setPackageSeq(2);
        DesignPackageEntity seq5 = new DesignPackageEntity();
        seq5.setPackageSeq(5);
        when(packageMapper.selectList(any())).thenReturn(List.of(seq2, seq5));

        assertThat(packageService.getNextPackageSeq(10L)).isEqualTo(6);
    }

    @Test
    void packageNextSequence_returnsOneWhenNoPackageHasSequence() {
        when(packageMapper.selectList(any())).thenReturn(List.of());

        assertThat(packageService.getNextPackageSeq(10L)).isEqualTo(1);
    }

    @Test
    void specReferenceChecker_returnsTrueWhenReferenced() {
        when(productMapper.selectCount(any())).thenReturn(1L);

        assertThat(specReferenceChecker.isSpecInUse(99L)).isTrue();
    }

    @Test
    void specReferenceChecker_returnsFalseWhenNotReferenced() {
        when(productMapper.selectCount(any())).thenReturn(0L);

        assertThat(specReferenceChecker.isSpecInUse(99L)).isFalse();
    }

    @Test
    void productFileListByProductIds_returnsEmptyForEmptyInput() {
        assertThat(productFileService.listByProductIds(List.of())).isEmpty();
        verifyNoInteractions(productFileMapper, productMapper);
    }

    @Test
    void filledPackageFileIds_returnsEmptyForEmptyPackageIds() {
        assertThat(productFileService.getFilledPackageFileIds(List.of())).isEmpty();
        verifyNoInteractions(productFileMapper, productMapper);
    }
}
