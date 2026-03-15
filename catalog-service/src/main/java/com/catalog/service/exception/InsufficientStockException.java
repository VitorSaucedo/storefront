package com.catalog.service.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, Integer available, Integer requested) {
        super("Estoque insuficiente para o produto id=" + productId +
                ". Disponível: " + available + ", Solicitado: " + requested);
    }
}
