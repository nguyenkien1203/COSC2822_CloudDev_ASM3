package com.restaurant.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
	"com.restaurant.authservice",
	"com.restaurant.redismodule",
	"com.restaurant.factorymodule",
	"com.restaurant.kafkamodule",
	"com.restaurant.sqsmodule"
})
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
