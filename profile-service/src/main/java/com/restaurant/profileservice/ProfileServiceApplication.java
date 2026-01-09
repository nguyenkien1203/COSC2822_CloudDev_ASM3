package com.restaurant.profileservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
		"com.restaurant.profileservice",
		"com.restaurant.redismodule",
		"com.restaurant.factorymodule",
		"com.restaurant.kafkamodule",
		"com.restaurant.securitymodule",
		"com.restaurant.sqsmodule"
})
public class ProfileServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProfileServiceApplication.class, args);
	}

}
