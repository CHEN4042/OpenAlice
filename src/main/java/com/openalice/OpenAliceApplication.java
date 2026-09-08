package com.openalice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** OpenAlice 启动类：Spring 容器即本应用的组合根。 */
@SpringBootApplication
public class OpenAliceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenAliceApplication.class, args);
    }
}
