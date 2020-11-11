package com.shopizer.inventory.csv.in;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import lombok.extern.log4j.Log4j2;

@Log4j2
@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class ImportApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImportApplication.class, args);
        log.info("=================================");
        log.info("Helo ImportApplication!");
        log.info("------- Initialization Complete -----------");
        log.debug("-------debug Initialization Complete -----------");
        log.trace("-------trace Initialization Complete -----------");
        log.error("-------error Initialization Complete -----------");	}

}
