package com.yuji;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.yuji.**.mapper")
public class YujiServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(YujiServerApplication.class, args);
    }
}
