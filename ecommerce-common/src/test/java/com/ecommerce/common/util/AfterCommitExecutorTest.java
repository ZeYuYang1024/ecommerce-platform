package com.ecommerce.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AfterCommitExecutorTest {

    @Test
    void shouldRunImmediatelyWhenNoTransactionSynchronizationIsActive() {
        AtomicInteger counter = new AtomicInteger();

        AfterCommitExecutor.run(counter::incrementAndGet);

        assertThat(counter).hasValue(1);
    }

    @Test
    void shouldDeferExecutionUntilAfterCommitWhenTransactionSynchronizationIsActive() {
        AtomicInteger counter = new AtomicInteger();
        TransactionSynchronizationManager.initSynchronization();
        try {
            AfterCommitExecutor.run(counter::incrementAndGet);

            assertThat(counter).hasValue(0);
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            assertThat(counter).hasValue(1);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
