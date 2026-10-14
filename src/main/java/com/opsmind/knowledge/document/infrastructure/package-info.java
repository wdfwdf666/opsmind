/**
 * 知识库模块的外部技术实现。
 * <p>包含 JDBC 数据库访问、本地文件存储、Apache Tika 解析，以及 Qwen/本地模型适配器。
 * 类上的 {@code @Repository}/{@code @Component} 使实现被 Spring 管理并注入到 application 层接口。</p>
 */
package com.opsmind.knowledge.document.infrastructure;
