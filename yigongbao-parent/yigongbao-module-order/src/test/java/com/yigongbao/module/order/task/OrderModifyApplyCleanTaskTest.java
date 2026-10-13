package com.yigongbao.module.order.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yigongbao.module.order.entity.OrderModificationApplyEntity;
import com.yigongbao.module.order.mapper.OrderModificationApplyMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderModifyApplyCleanTaskTest {

    @BeforeAll
    static void initLambdaCache() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, OrderModificationApplyEntity.class);
    }

    @Mock
    private OrderModificationApplyMapper applyMapper;

    @Test
    void cleanExpiredApplications_onlyMarksStatusAndKeepsHistoryContent() {
        when(applyMapper.expireApplicationsForChangedPhase()).thenReturn(2);
        when(applyMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        new OrderModifyApplyCleanTask(applyMapper).cleanExpiredApplications();

        ArgumentCaptor<LambdaUpdateWrapper<OrderModificationApplyEntity>> captor =
            ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(applyMapper).update(isNull(), captor.capture());
        verify(applyMapper).expireApplicationsForChangedPhase();
        LambdaUpdateWrapper<OrderModificationApplyEntity> wrapper = captor.getValue();

        assertThat(wrapper.getSqlSet()).contains("status");
        assertThat(wrapper.getSqlSet()).doesNotContain("modification_content", "modification_diff");
        assertThat(wrapper.getExpression().getNormal()).hasSize(7);
    }
}
