package com.ecommerce.common.transaction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DistributedTransactionContextTest {

    @Test
    void shouldAdvanceThroughProcessingToSuccess() {
        DistributedTransactionContext context = DistributedTransactionContext.start("tx-1", "ORD-1", "idem-1");

        assertThat(context.getStatus()).isEqualTo(DistributedTransactionStatus.INIT);

        context.apply(DistributedTransactionEvent.BEGIN, "create-order", null);
        assertThat(context.getStatus()).isEqualTo(DistributedTransactionStatus.PROCESSING);
        assertThat(context.getCurrentStep()).isEqualTo("create-order");

        context.apply(DistributedTransactionEvent.COMPLETE, "publish-event", null);
        assertThat(context.getStatus()).isEqualTo(DistributedTransactionStatus.SUCCESS);
        assertThat(context.getCurrentStep()).isEqualTo("publish-event");
        assertThat(context.getErrorMessage()).isNull();
    }

    @Test
    void terminalStateShouldIgnoreRepeatedEvents() {
        DistributedTransactionContext context = DistributedTransactionContext.start("tx-2", "ORD-2", "idem-2");

        context.apply(DistributedTransactionEvent.BEGIN, "reserve-stock", null);
        context.apply(DistributedTransactionEvent.FAIL, "reserve-stock", "inventory down");

        assertThat(context.getStatus()).isEqualTo(DistributedTransactionStatus.FAILED);
        assertThat(context.getCurrentStep()).isEqualTo("reserve-stock");
        assertThat(context.getErrorMessage()).isEqualTo("inventory down");

        context.apply(DistributedTransactionEvent.COMPENSATE, "cancel-order", "should be ignored");

        assertThat(context.getStatus()).isEqualTo(DistributedTransactionStatus.FAILED);
        assertThat(context.getCurrentStep()).isEqualTo("reserve-stock");
        assertThat(context.getErrorMessage()).isEqualTo("inventory down");
    }
}
