package com.personal.jz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.personal.jz.repository")
public class JzApplication {
    public static void main(String[] args) {
        SpringApplication.run(JzApplication.class, args);
    }
}
