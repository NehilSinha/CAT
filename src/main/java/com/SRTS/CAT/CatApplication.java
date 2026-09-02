package com.SRTS.CAT;

import com.SRTS.CAT.util.EnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CatApplication {
	public static void main(String[] args) {
		EnvLoader.applyToSystemProperties("MONGODB_URI", "GROQ_API_KEY");
		SpringApplication.run(CatApplication.class, args);
	}
}
