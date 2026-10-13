package br.com.alurafood.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "SERVER_HOST=http://localhost:8081/eureka")
class ServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
