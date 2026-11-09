package com.yuji.auth;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 测试专用启动类：yuji-auth 是库模块，没有 @SpringBootApplication，
 * 而 @WebMvcTest 需要一个可发现的配置入口。仅用于测试切片，不参与打包。
 */
@SpringBootApplication
public class TestApplication {
}
