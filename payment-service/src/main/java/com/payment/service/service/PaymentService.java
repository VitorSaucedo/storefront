package com.payment.service.service;

import com.payment.service.domain.Payment;
import com.payment.service.domain.PaymentStatus;
import com.payment.service.dto.PaymentResponse;
import com.payment.service.dto.events.OrderCreatedEvent;
import com.payment.service.dto.events.PaymentFailedEvent;
import com.payment.service.dto.events.PaymentProcessedEvent;
import com.payment.service.exception.PaymentNotFoundException;
import com.payment.service.messaging.PaymentEventPublisher;
import com.payment.service.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Random;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    private final Random random = new Random();

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void processPayment(OrderCreatedEvent event) {
        log.info("Processing payment for orderId={}, amount={}", event.orderId(), event.totalAmount());

        if (paymentRepository.findByOrderId(event.orderId()).isPresent()) {
            log.warn("Payment already exists for orderId={}, skipping", event.orderId());
            return;
        }

        Payment payment = new Payment(event.orderId(), event.userId(), event.totalAmount());
        paymentRepository.save(payment);

        boolean success = random.nextInt(10) < 8;

        if (success) {
            payment.setStatus(PaymentStatus.PROCESSED);
            paymentRepository.save(payment);

            eventPublisher.publishPaymentProcessed(new PaymentProcessedEvent(
                    event.orderId(),
                    event.userId(),
                    payment.getId(),
                    event.totalAmount()
            ));

            log.info("Payment processed successfully for orderId={}", event.orderId());
        } else {
            String reason = "Insufficient funds";
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            paymentRepository.save(payment);

            eventPublisher.publishPaymentFailed(new PaymentFailedEvent(
                    event.orderId(),
                    event.userId(),
                    reason
            ));

            log.warn("Payment failed for orderId={}, reason={}", event.orderId(), reason);
        }
    }

    public Page<PaymentResponse> getPaymentsByUser(Long userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    public PaymentResponse getPaymentByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> PaymentNotFoundException.byOrderId(orderId));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getFailureReason()
        );
    }
}
