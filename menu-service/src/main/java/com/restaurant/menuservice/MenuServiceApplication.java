package com.restaurant.menuservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

@SpringBootApplication
@ComponentScan(basePackages = { "com.restaurant.menuservice",
		"com.restaurant.redismodule",
		"com.restaurant.kafkamodule",
		"com.restaurant.factorymodule",
		"com.restaurant.securitymodule",
		"com.restaurant.sqsmodule" })
public class MenuServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MenuServiceApplication.class, args);
	}

}
