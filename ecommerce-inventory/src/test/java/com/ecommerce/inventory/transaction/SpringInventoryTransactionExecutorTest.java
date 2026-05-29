package com.ecommerce.inventory.transaction;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SpringInventoryTransactionExecutorTest {

    @Test
    void execute_shouldAlwaysStartRequiresNewTransaction() {
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        SpringInventoryTransactionExecutor executor = new SpringInventoryTransactionExecutor(transactionManager);
        AtomicBoolean actionExecuted = new AtomicBoolean(false);

        executor.execute(() -> actionExecuted.set(true));

        assertThat(actionExecuted).isTrue();
        assertThat(transactionManager.propagationBehavior)
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(transactionManager.commits).isEqualTo(1);
        assertThat(transactionManager.rollbacks).isZero();
    }

    private static class RecordingTransactionManager implements PlatformTransactionManager {

        private Integer propagationBehavior;
        private int commits;
        private int rollbacks;

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            this.propagationBehavior = definition.getPropagationBehavior();
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            commits++;
        }

        @Override
        public void rollback(TransactionStatus status) {
            rollbacks++;
        }
    }
}
