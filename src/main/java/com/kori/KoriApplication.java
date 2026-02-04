package com.kori;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KoriApplication {

	public static void main(String[] args) {
		SpringApplication.run(KoriApplication.class, args);
	}

}
