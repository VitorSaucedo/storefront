package com.auth.service;

import com.auth.service.repository.UserRepository;
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
class AuthServiceApplicationTests {

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private ConnectionFactory connectionFactory;

	@Test
	void contextLoads() {
	}
}