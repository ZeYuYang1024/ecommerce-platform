package com.ecommerce.member.service.impl;

import com.ecommerce.member.entity.MemberLevel;
import com.ecommerce.member.entity.MemberProfile;
import com.ecommerce.member.entity.PointsConsumeDetail;
import com.ecommerce.member.entity.PointsReservation;
import com.ecommerce.member.entity.PointsTransaction;
import com.ecommerce.member.mapper.MemberLevelMapper;
import com.ecommerce.member.mapper.MemberProfileMapper;
import com.ecommerce.member.mapper.PointsConsumeDetailMapper;
import com.ecommerce.member.mapper.PointsReservationMapper;
import com.ecommerce.member.mapper.PointsTransactionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointsServiceImplTest {

    @Mock
    private PointsTransactionMapper pointsTransactionMapper;

    @Mock
    private MemberProfileMapper memberProfileMapper;

    @Mock
    private MemberLevelMapper memberLevelMapper;

    @Mock
    private PointsReservationMapper pointsReservationMapper;

    @Mock
    private PointsConsumeDetailMapper pointsConsumeDetailMapper;

    @InjectMocks
    private PointsServiceImpl service;

    @Test
    void spendShouldWriteSpendLedgerAndIncreaseTotalSpentPoints() {
        MemberProfile profile = buildProfile(1L, 10001L, 380L, 200L, 2L);
        PointsReservation reservation = buildReservation(10L, "PR1", "ORD-1", 10001L);
        PointsTransaction earnTx1 = buildEarnTx(101L, 10001L, 100, 20, LocalDateTime.now().plusDays(1));
        PointsTransaction earnTx2 = buildEarnTx(102L, 10001L, 80, 0, LocalDateTime.now().plusDays(2));
        when(pointsTransactionMapper.selectCount(any())).thenReturn(0L);
        when(memberProfileMapper.selectOne(any())).thenReturn(profile);
        when(memberProfileMapper.update(any(), any())).thenReturn(1);
        when(pointsReservationMapper.selectOne(any())).thenReturn(reservation);
        when(pointsTransactionMapper.selectList(any())).thenReturn(List.of(earnTx1, earnTx2));

        service.spend(10001L, 120, "ORDER", "ORD-1", "confirm:ORD-1",
                "\u8ba2\u5355\u79ef\u5206\u62b5\u6263", "PR1");

        verify(memberProfileMapper).update(any(), any());
        verify(pointsTransactionMapper, times(2)).updateById(any(PointsTransaction.class));
        verify(pointsConsumeDetailMapper, times(2)).insert(isA(PointsConsumeDetail.class));

        ArgumentCaptor<PointsTransaction> txCaptor = ArgumentCaptor.forClass(PointsTransaction.class);
        verify(pointsTransactionMapper).insert(txCaptor.capture());
        PointsTransaction tx = txCaptor.getValue();
        assertThat(tx.getDirection()).isEqualTo("SPEND");
        assertThat(tx.getAmount()).isEqualTo(120);
        assertThat(tx.getBalanceAfter()).isEqualTo(380L);
        assertThat(tx.getSourceType()).isEqualTo("ORDER");
        assertThat(tx.getSourceId()).isEqualTo("ORD-1");
        assertThat(tx.getBizKey()).isEqualTo("confirm:ORD-1");
        assertThat(tx.getRemark()).isEqualTo("\u8ba2\u5355\u79ef\u5206\u62b5\u6263");
        assertThat(tx.getRelatedReservationNo()).isEqualTo("PR1");
        assertThat(tx.getReversalOfTxId()).isNull();
        assertThat(earnTx1.getConsumedAmount()).isEqualTo(100);
        assertThat(earnTx2.getConsumedAmount()).isEqualTo(40);
    }

    @Test
    void reverseSpendShouldRestoreAvailablePointsAndWriteEarnLedger() {
        MemberProfile profile = buildProfile(1L, 10001L, 380L, 200L, 2L);
        when(pointsTransactionMapper.selectCount(any())).thenReturn(0L);
        when(memberProfileMapper.selectOne(any())).thenReturn(profile);
        when(memberProfileMapper.update(any(), any())).thenReturn(1);
        when(memberProfileMapper.selectById(1L)).thenReturn(buildProfile(1L, 10001L, 500L, 200L, 3L));

        service.reverseSpend(10001L, 120, "REFUND", "RF1", "REFUND:RF1:POINTS",
                "\u9000\u6b3e\u9000\u56de\u79ef\u5206", "PR1", 9001L);

        verify(memberProfileMapper).update(any(), any());
        ArgumentCaptor<PointsTransaction> txCaptor = ArgumentCaptor.forClass(PointsTransaction.class);
        verify(pointsTransactionMapper).insert(txCaptor.capture());
        PointsTransaction tx = txCaptor.getValue();
        assertThat(tx.getDirection()).isEqualTo("EARN");
        assertThat(tx.getAmount()).isEqualTo(120);
        assertThat(tx.getBalanceAfter()).isEqualTo(500L);
        assertThat(tx.getSourceType()).isEqualTo("REFUND");
        assertThat(tx.getSourceId()).isEqualTo("RF1");
        assertThat(tx.getBizKey()).isEqualTo("REFUND:RF1:POINTS");
        assertThat(tx.getRemark()).isEqualTo("\u9000\u6b3e\u9000\u56de\u79ef\u5206");
        assertThat(tx.getRelatedReservationNo()).isEqualTo("PR1");
        assertThat(tx.getReversalOfTxId()).isEqualTo(9001L);
    }

    @Test
    void earnShouldLeaveReservationFieldsNullForPhase1Scenarios() {
        MemberProfile profile = buildProfile(1L, 10001L, 380L, 200L, 2L);
        MemberLevel level = new MemberLevel();
        level.setId(11L);
        level.setPointsMultiplier(BigDecimal.ONE);
        when(pointsTransactionMapper.selectCount(any())).thenReturn(0L);
        when(memberProfileMapper.selectOne(any())).thenReturn(profile);
        when(memberLevelMapper.selectById(11L)).thenReturn(level);
        when(memberProfileMapper.update(any(), any())).thenReturn(1);
        when(memberProfileMapper.selectById(1L)).thenReturn(buildProfile(1L, 10001L, 390L, 200L, 3L));

        service.earn(10001L, 10, "CHECKIN", "CHK-1", "checkin:1", "checkin");

        ArgumentCaptor<PointsTransaction> txCaptor = ArgumentCaptor.forClass(PointsTransaction.class);
        verify(pointsTransactionMapper).insert(txCaptor.capture());
        PointsTransaction tx = txCaptor.getValue();
        assertThat(tx.getRelatedReservationNo()).isNull();
        assertThat(tx.getReversalOfTxId()).isNull();
    }

    @Test
    void spendShouldIgnoreDuplicateBizKey() {
        when(pointsTransactionMapper.selectCount(any())).thenReturn(1L);

        service.spend(10001L, 120, "ORDER", "ORD-1", "confirm:ORD-1",
                "\u8ba2\u5355\u79ef\u5206\u62b5\u6263", "PR1");

        verify(memberProfileMapper, never()).selectOne(any());
        verify(pointsTransactionMapper, never()).insert(isA(PointsTransaction.class));
    }

    private MemberProfile buildProfile(Long id, Long userId, Long availablePoints, Long totalSpentPoints, Long version) {
        MemberProfile profile = new MemberProfile();
        profile.setId(id);
        profile.setUserId(userId);
        profile.setAvailablePoints(availablePoints);
        profile.setTotalSpentPoints(totalSpentPoints);
        profile.setLevelId(11L);
        profile.setVersion(version);
        return profile;
    }

    private PointsReservation buildReservation(Long id, String reservationNo, String orderNo, Long userId) {
        PointsReservation reservation = new PointsReservation();
        reservation.setId(id);
        reservation.setReservationNo(reservationNo);
        reservation.setOrderNo(orderNo);
        reservation.setUserId(userId);
        return reservation;
    }

    private PointsTransaction buildEarnTx(Long id, Long userId, int amount, int consumedAmount, LocalDateTime expireAt) {
        PointsTransaction tx = new PointsTransaction();
        tx.setId(id);
        tx.setUserId(userId);
        tx.setDirection("EARN");
        tx.setAmount(amount);
        tx.setConsumedAmount(consumedAmount);
        tx.setExpireAt(expireAt);
        tx.setSourceType("ORDER");
        tx.setSourceId("SRC-" + id);
        tx.setBizKey("BIZ-" + id);
        return tx;
    }
}
