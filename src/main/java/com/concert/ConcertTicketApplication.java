package com.concert;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @description:    演唱会订票系统启动类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@SpringBootApplication
@MapperScan("com.concert.mapper")
@EnableScheduling // 启用定时任务
@EnableAsync // 启用异步方法
public class ConcertTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConcertTicketApplication.class, args);
    }
}
