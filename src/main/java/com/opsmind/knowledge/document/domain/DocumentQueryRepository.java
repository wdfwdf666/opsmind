package com.opsmind.knowledge.document.domain; // 包作用：领域层，定义核心业务对象、状态和仓储契约。

public interface DocumentQueryRepository { // 类型作用：仓储接口，业务层依赖它而不是依赖具体 SQL。

    DocumentPage findPage(int page, int size); // 接口约定：规定实现类必须按条件读取目标数据，不存在时按约定返回空值或异常。
}
