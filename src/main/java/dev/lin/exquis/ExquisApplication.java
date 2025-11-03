package dev.lin.exquis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExquisApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExquisApplication.class, args);
	}

}
