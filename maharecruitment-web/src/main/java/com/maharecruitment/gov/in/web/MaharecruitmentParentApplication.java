package com.maharecruitment.gov.in.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
		"com.maharecruitment.gov.in",
		"com.project"
})
@EnableJpaRepositories(basePackages = {
		"com.maharecruitment.gov.in",
		"com.project"
})
@EntityScan(basePackages = {
		"com.maharecruitment.gov.in",
		"com.project"
})
public class MaharecruitmentParentApplication {

	public static void main(String[] args) {
		SpringApplication.run(MaharecruitmentParentApplication.class, args);
	}

	@Profile("local")
	@Bean
	String devBean() {
		return "local";
	}

	@Profile("uat")
	@Bean
	String qaBean() {
		return "uat";
	}

	@Profile("prod")
	@Bean
	String prodBean() {
		return "prod";
	}

}
