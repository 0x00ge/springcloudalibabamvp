package com.mvp.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Author zt
 */
@MapperScan("com.mvp.user.mapper")
@SpringBootApplication(scanBasePackages = {"com.mvp.user", "com.mvp.common"})
public class UserApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(UserApplication.class, args);
    }
}
