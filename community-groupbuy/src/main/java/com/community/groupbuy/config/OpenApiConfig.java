package com.community.groupbuy.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI3 文档配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI groupBuyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("社区团购（生鲜自提）拼团系统 API")
                        .description("Java 开发综合实训 · 案例三 · 提供商品/开团/参团下单/支付成团/自提核销/统计导出等接口")
                        .version("v1.0.0")
                        .contact(new Contact().name("社区团购小组")));
    }
}
