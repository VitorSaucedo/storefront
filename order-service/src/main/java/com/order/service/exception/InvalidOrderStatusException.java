package com.order.service.exception;

public class InvalidOrderStatusException extends RuntimeException {

    public InvalidOrderStatusException(Long orderId, String currentStatus, String action) {
        super("Não é possível " + action + " o pedido id=" + orderId +
                ". Status atual: " + currentStatus);
    }
}