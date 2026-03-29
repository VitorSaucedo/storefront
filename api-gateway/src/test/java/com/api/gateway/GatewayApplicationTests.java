package com.api.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.autoconfigure.exclude=" +
				"org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration," +
				"org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration",
		"jwt.secret=3f8a2b1c9d4e7f6a0b5c8d2e1f4a7b3c9d6e2f5a8b1c4d7e0f3a6b9c2d5e8f1a",
		"app.security.public-paths=/auth/register,/auth/login,/actuator"
})
class GatewayApplicationTests {
	@Test
	void contextLoads() {}
}
