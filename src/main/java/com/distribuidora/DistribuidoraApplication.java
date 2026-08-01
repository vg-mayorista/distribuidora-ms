package com.distribuidora;

import com.distribuidora.config.StockProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(StockProperties.class)
public class DistribuidoraApplication {

	public static void main(String[] args) {
		SpringApplication.run(DistribuidoraApplication.class, args);
	}

}
