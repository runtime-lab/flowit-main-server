package dev.runtime_lab.flowit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class FlowitApplicationTests {

	@Container
	@ServiceConnection
	static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
		.withDatabaseName("project_flowit")
		.withUsername("flowit_test")
		.withPassword("flowitTestPass");

	@Test
	void contextLoads() {
	}

}
