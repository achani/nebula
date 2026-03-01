package com.nebula.code;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class CodeServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(CodeServiceApplication.class, args);
  }
}
