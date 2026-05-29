package com.ecommerce.inventory.transaction;

@FunctionalInterface
public interface InventoryTransactionExecutor {
    void execute(Runnable action);
}
