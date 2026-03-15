package com.notecurve;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class NotecurveApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotecurveApplication.class, args);
	}

}
