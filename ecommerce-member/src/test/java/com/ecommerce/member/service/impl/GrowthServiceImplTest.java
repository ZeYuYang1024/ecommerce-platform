package com.ecommerce.member.service.impl;

import com.ecommerce.member.entity.MemberProfile;
import com.ecommerce.member.entity.GrowthTransaction;
import com.ecommerce.member.mapper.GrowthTransactionMapper;
import com.ecommerce.member.mapper.MemberLevelMapper;
import com.ecommerce.member.mapper.MemberProfileMapper;
import com.ecommerce.member.service.LevelService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrowthServiceImplTest {

    @Mock
    private GrowthTransactionMapper growthTransactionMapper;

    @Mock
    private MemberProfileMapper memberProfileMapper;

    @Mock
    private MemberLevelMapper memberLevelMapper;

    @Mock
    private LevelService levelService;

    @InjectMocks
    private GrowthServiceImpl service;

    @Test
    void addShouldRecordNegativeGrowthAdjustmentForRefundWithoutUpgrade() {
        MemberProfile profile = new MemberProfile();
        profile.setId(1L);
        profile.setUserId(10001L);
        profile.setGrowthValue(199L);
        profile.setTotalGrowthValue(300L);
        profile.setVersion(2L);
        when(growthTransactionMapper.selectCount(any())).thenReturn(0L);
        when(memberProfileMapper.selectOne(any())).thenReturn(profile);
        when(memberProfileMapper.update(any(), any())).thenReturn(1);
        MemberProfile updated = new MemberProfile();
        updated.setId(1L);
        updated.setUserId(10001L);
        updated.setGrowthValue(0L);
        updated.setTotalGrowthValue(300L);
        when(memberProfileMapper.selectById(1L)).thenReturn(updated);

        service.add(10001L, -199, "REFUND", "RF1", "REFUND:RF1:GROWTH",
                "\u9000\u6b3e\u6263\u56de\u6210\u957f\u503c");

        verify(memberProfileMapper).update(any(), any());
        verify(levelService, never()).checkUpgrade(any(), any());
        ArgumentCaptor<GrowthTransaction> txCaptor = ArgumentCaptor.forClass(GrowthTransaction.class);
        verify(growthTransactionMapper).insert(txCaptor.capture());
        GrowthTransaction tx = txCaptor.getValue();
        assertThat(tx.getAmount()).isEqualTo(-199);
        assertThat(tx.getBalanceAfter()).isEqualTo(0L);
        assertThat(tx.getSourceType()).isEqualTo("REFUND");
        assertThat(tx.getSourceId()).isEqualTo("RF1");
        assertThat(tx.getBizKey()).isEqualTo("REFUND:RF1:GROWTH");
    }
}
