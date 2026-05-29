package com.ecommerce.inventory.transaction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SpringInventoryTransactionExecutor implements InventoryTransactionExecutor {

    private final TransactionTemplate transactionTemplate;

    public SpringInventoryTransactionExecutor(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void execute(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }
}
