package br.com.alurafood.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "SERVER_HOST=http://localhost:8081/eureka")
class GatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
