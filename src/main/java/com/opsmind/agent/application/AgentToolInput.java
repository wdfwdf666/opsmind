package com.opsmind.agent.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import java.util.UUID;

public record AgentToolInput(String query /* 字段作用：用户输入的问题或检索文本。 */, UUID documentId /* 字段作用：对应对象的唯一标识。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。
}
