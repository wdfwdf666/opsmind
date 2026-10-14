package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import java.io.InputStream;

public interface DocumentParser { // 类型作用：接口，只定义能力契约，实际逻辑由实现类完成。

    ParsedDocument parse(InputStream inputStream, String filename, String declaredMediaType); // 接口约定：规定实现类必须把外部或数据库数据转换成项目内部对象。
}
