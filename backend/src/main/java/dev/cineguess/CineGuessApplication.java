package dev.cineguess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CineGuessApplication {

    public static void main(String[] args) {
        SpringApplication.run(CineGuessApplication.class, args);
    }
}
