package com.opsmind.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger/OpenAPI 的全局元数据配置，决定接口文档页面显示的标题、版本和说明。 */
// @Configuration 表示这是配置类，Spring 启动时会执行其中的 @Bean 方法。
@Configuration
public class OpenApiConfiguration {

    // @Bean 把返回的 OpenAPI 对象登记进 Spring 容器，springdoc 会自动使用它。
    @Bean
    public OpenAPI opsMindOpenApi() {
        return new OpenAPI().info(new Info()
                .title("OpsMind API")
                .version("v1")
                .description("Enterprise knowledge base and incident-handling RAG Agent API"));
    }
}

