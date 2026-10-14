package com.opsmind; // 包作用：项目根包，Spring Boot 从这里向下扫描组件。

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 整个 OpsMind 项目的启动入口。
 * <p>
 * Java 程序从 {@link #main(String[])} 方法开始执行。这里没有手工创建 Controller、Service，
 * 是因为 Spring 会负责对象的创建、依赖注入和生命周期管理，这个由 Spring 管理的对象集合通常叫作
 * <strong>IoC 容器</strong>。
 * </p>
 */
// @SpringBootApplication 是三个注解的组合：
// 1. @Configuration：当前类可以声明 Spring 配置；
// 2. @EnableAutoConfiguration：根据依赖和配置自动装配 Web、JDBC、Actuator 等能力；
// 3. @ComponentScan：从 com.opsmind 开始扫描 @Controller、@Service、@Component 等组件。
@SpringBootApplication // 注解作用：组合启动注解，开启自动配置并扫描当前包及子包。
// 扫描 @ConfigurationProperties 类型，把 application.yml/.env 配置转换成类型安全的 Java 对象。
@ConfigurationPropertiesScan // 注解作用：扫描类型安全配置对象并完成外部配置绑定。
public class OpsMindApplication { // 类型作用：封装 OpsMindApplication 对应的单一职责，供当前模块协作使用。

	/**
	 * 应用入口。SpringApplication.run 会启动 Spring 容器和内嵌 Tomcat，随后 8080 端口开始接收请求。
	 *
	 * @param args 命令行参数，例如可传入 {@code --server.port=8081} 临时覆盖配置
	 */
	public static void main(String[] args) { // 方法作用：启动 Spring 容器、自动配置和内嵌 Web 服务器。
		SpringApplication.run(OpsMindApplication.class, args);
	}

}
