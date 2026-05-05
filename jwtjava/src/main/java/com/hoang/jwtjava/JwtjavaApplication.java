package com.hoang.jwtjava;

import com.hoang.jwtjava.config.CorsProperties;
import com.hoang.jwtjava.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({StorageProperties.class, CorsProperties.class})
public class JwtjavaApplication {

	public static void main(String[] args) {
		SpringApplication.run(JwtjavaApplication.class, args);
	}

}
