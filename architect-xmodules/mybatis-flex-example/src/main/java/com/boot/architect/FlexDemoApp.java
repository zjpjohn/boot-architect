package com.boot.architect;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.boot.architect.infrast.persist.mapper")
public class FlexDemoApp {

    static void main(String[] args) {
        SpringApplication.run(FlexDemoApp.class, args);
    }

}
