package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import java.util.UUID;

public record DocumentChunkingResult(
        UUID documentId /* 字段作用：对应对象的唯一标识。 */,
        int parentCount /* 字段作用：对应项目的数量统计。 */,
        int childCount /* 字段作用：对应项目的数量统计。 */) { // 类型作用：不可变数据对象，Java 自动生成构造器、访问方法、equals 和 hashCode。

    public int totalCount() { // 方法作用：把外部或数据库数据转换成项目内部对象。
        return parentCount + childCount;
    }
}
