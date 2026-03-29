package com.order.service;

import com.order.service.repository.OrderRepository;
import com.order.service.repository.OutboxRepository;
import com.order.service.service.OutboxService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.autoconfigure.exclude=" +
				"org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
				"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
				"org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration"
})
class OrderServiceApplicationTests {

	@MockitoBean
	private OrderRepository orderRepository;

	@MockitoBean
	private OutboxRepository outboxRepository;

	@MockitoBean
	private OutboxService outboxService;

	@MockitoBean
	private ConnectionFactory connectionFactory;

	@Test
	void contextLoads() {
	}
}