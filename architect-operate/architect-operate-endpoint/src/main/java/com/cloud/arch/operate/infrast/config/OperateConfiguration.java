package com.cloud.arch.operate.infrast.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 此配置目的spring应用扫描到包内bean
 */
@Configuration
@ComponentScan(basePackages = "com.cloud.arch.operate")
public class OperateConfiguration {
}
