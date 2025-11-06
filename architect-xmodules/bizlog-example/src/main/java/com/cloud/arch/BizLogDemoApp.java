package com.cloud.arch;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cloud.arch.infrast.persist.mapper")
public class BizLogDemoApp {

    public static void main(String[] args) {
        SpringApplication.run(BizLogDemoApp.class, args);
    }

}
