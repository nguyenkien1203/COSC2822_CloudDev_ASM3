package com.restaurant.reservationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
		"com.restaurant.reservationservice",
		"com.restaurant.redismodule",
		"com.restaurant.factorymodule",
		"com.restaurant.kafkamodule",
		"com.restaurant.securitymodule",
		"com.restaurant.sqsmodule"
})
public class ReservationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservationServiceApplication.class, args);
	}

}
