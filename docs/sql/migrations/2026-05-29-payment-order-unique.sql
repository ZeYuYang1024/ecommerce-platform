ALTER TABLE payment
    ADD UNIQUE KEY uk_payment_order_no (order_no);
