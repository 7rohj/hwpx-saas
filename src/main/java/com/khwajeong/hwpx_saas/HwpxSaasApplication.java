package com.khwajeong.hwpx_saas;

import com.khwajeong.hwpx_saas.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// @EnableConfigurationProperties(AppProperties.class)
@ConfigurationPropertiesScan("com.khwajeong.hwpx_saas.config")
@SpringBootApplication
public class HwpxSaasApplication {

	public static void main(String[] args) {
		SpringApplication.run(HwpxSaasApplication.class, args);
	}

}
