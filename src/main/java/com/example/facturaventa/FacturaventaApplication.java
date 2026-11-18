package com.example.facturaventa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.boot.context.properties.EnableConfigurationProperties(com.example.facturaventa.arca.ArcaConfig.class)
public class FacturaventaApplication {

	public static void main(String[] args) {
		SpringApplication.run(FacturaventaApplication.class, args);
	}

}
