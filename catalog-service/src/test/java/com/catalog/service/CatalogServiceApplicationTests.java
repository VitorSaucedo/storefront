package com.catalog.service;

import com.catalog.service.repository.ProductRepository;
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
class CatalogServiceApplicationTests {

	@MockitoBean
	private ProductRepository productRepository;

	@MockitoBean
	private ConnectionFactory connectionFactory;

	@Test
	void contextLoads() {
	}
}
