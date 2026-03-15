package com.payment.service.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }

    public static PaymentNotFoundException byOrderId(Long orderId) {
        return new PaymentNotFoundException("Payment not found for orderId=" + orderId);
    }
}
