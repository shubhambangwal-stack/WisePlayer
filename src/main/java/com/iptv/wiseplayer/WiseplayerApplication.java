package com.iptv.wiseplayer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WiseplayerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WiseplayerApplication.class, args);
	}

}
