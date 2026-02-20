package com.sparktech.motorx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class MotorxApplication {

	public static void main(String[] args) {
		// Habilita debug de Spring Security en tiempo de ejecución (útil si el IDE muestra advertencias sobre la propiedad)
		SpringApplication app = new SpringApplication(MotorxApplication.class);
		app.setDefaultProperties(Map.of("spring.security.debug", "true"));
		app.run(args);
	}

}
