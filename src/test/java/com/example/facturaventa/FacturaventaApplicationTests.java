package com.example.facturaventa;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:contexto;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","app.demo-data.enabled=false"})
class FacturaventaApplicationTests {

	@Test
	void contextLoads() {
	}

}

