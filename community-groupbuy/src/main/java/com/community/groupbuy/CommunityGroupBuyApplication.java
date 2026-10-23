package com.community.groupbuy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 社区团购（生鲜自提）拼团系统 启动类
 */
@SpringBootApplication
@MapperScan("com.community.groupbuy.mapper")
@EnableAsync
@EnableScheduling
public class CommunityGroupBuyApplication {

    public static void main(String[] args) {
        // JDK 安全策略默认禁止 XML 解析器读取外部 DTD，会导致 MyBatis mapper XML 解析失败；
        // 必须在 Spring 容器初始化（MyBatis 解析 XML）之前放开
        System.setProperty("javax.xml.accessExternalDTD", "all");
        SpringApplication.run(CommunityGroupBuyApplication.class, args);
    }
}
