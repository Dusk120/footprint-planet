package com.qzxx.footprint_planet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.qzxx.footprint_planet.mapper")
public class FootprintPlanetApplication {

	public static void main(String[] args) {
		SpringApplication.run(FootprintPlanetApplication.class, args);
	}

}
