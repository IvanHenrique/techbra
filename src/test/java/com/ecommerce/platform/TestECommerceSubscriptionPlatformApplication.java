package com.ecommerce.platform;

import org.springframework.boot.SpringApplication;

public class TestECommerceSubscriptionPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.from(ECommerceSubscriptionPlatformApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
