package com.ecommerce.member.service.impl;

import com.ecommerce.member.entity.PointsTransaction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PointsReservationServiceImplTest {

    @Test
    void earnShouldLeaveReservationFieldsNullForPhase1Scenarios() {
        PointsTransaction tx = new PointsTransaction();
        tx.setId(11L);
        tx.setUserId(10001L);
        tx.setDirection("EARN");
        tx.setAmount(10);

        assertThat(tx.getRelatedReservationNo()).isNull();
        assertThat(tx.getReversalOfTxId()).isNull();
    }
}
