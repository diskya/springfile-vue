package com.example.springfile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // Enable asynchronous method execution
public class SpringfileApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringfileApplication.class, args);
	}

}
