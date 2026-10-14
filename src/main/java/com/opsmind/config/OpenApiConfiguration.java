package com.opsmind.config; // 包作用：配置层，把外部配置转换成 Spring 可管理的对象。

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger/OpenAPI 的全局元数据配置，决定接口文档页面显示的标题、版本和说明。 */
// @Configuration 表示这是配置类，Spring 启动时会执行其中的 @Bean 方法。
@Configuration // 注解作用：声明这是 Spring 配置类。
public class OpenApiConfiguration { // 类型作用：封装 OpenApiConfiguration 对应的单一职责，供当前模块协作使用。

    // @Bean 把返回的 OpenAPI 对象登记进 Spring 容器，springdoc 会自动使用它。
    @Bean // 注解作用：把方法返回对象注册到 Spring 容器。
    public OpenAPI opsMindOpenApi() { // 方法作用：完成“opsMindOpenApi”对应的内部步骤，供主流程复用。
        return new OpenAPI().info(new Info()
                .title("OpsMind API")
                .version("v1")
                .description("Enterprise knowledge base and incident-handling RAG Agent API"));
    }
}

