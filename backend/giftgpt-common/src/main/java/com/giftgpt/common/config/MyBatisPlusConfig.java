package com.giftgpt.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.giftgpt.**.mapper")
public class MyBatisPlusConfig {
}
