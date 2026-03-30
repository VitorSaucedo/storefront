package com.payment.service;

import com.payment.service.repository.OutboxRepository;
import com.payment.service.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.autoconfigure.exclude=" +
				"org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
				"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
				"org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
		"jwt.secret=3f8a2b1c9d4e7f6a0b5c8d2e1f4a7b3c9d6e2f5a8b1c4d7e0f3a6b9c2d5e8f1a"
})
class PaymentServiceApplicationTests {

	@MockitoBean
	private PaymentRepository paymentRepository;

	@MockitoBean
	private ConnectionFactory connectionFactory;

	@MockitoBean
	private OutboxRepository outboxRepository;

	@Test
	void contextLoads() {}
}
