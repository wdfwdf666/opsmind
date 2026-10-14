package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import java.io.InputStream;

public interface DocumentStorage { // 类型作用：接口，只定义能力契约，实际逻辑由实现类完成。

    String store(ValidatedDocument document); // 接口约定：规定实现类必须创建或保存数据，并返回持久化结果。

    InputStream open(String storageKey); // 接口约定：规定实现类必须完成“open”对应的内部步骤，供主流程复用。
}

