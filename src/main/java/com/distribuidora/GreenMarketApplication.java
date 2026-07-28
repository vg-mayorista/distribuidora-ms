package com.distribuidora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GreenMarketApplication {

	public static void main(String[] args) {
		SpringApplication.run(GreenMarketApplication.class, args);
	}

}
