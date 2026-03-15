package com.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.autoconfigure.exclude=" +
				"org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration," +
				"org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration," +
				"org.springframework.boot.security.autoconfigure.reactive.ReactiveSecurityAutoConfiguration," +
				"org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration"
})
class NotificationServiceApplicationTests {

	@MockitoBean
	private ConnectionFactory connectionFactory;

	@Test
	void contextLoads() {}
}