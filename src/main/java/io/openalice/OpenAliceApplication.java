package io.openalice;

import io.openalice.config.OpenAliceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(OpenAliceProperties.class)
public class OpenAliceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenAliceApplication.class, args);
    }
}
