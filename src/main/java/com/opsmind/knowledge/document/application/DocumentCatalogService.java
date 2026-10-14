package com.opsmind.knowledge.document.application; // 包作用：应用层，编排业务步骤，但不绑定具体数据库或模型厂商。

import com.opsmind.knowledge.document.domain.DocumentException;
import com.opsmind.knowledge.document.domain.DocumentPage;
import com.opsmind.knowledge.document.domain.DocumentQueryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service // 注解作用：把类注册为业务服务，由 Spring 负责创建和注入。
public class DocumentCatalogService { // 类型作用：应用服务，负责组织一个完整业务流程。

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private final DocumentQueryRepository queryRepository;

    public DocumentCatalogService(DocumentQueryRepository queryRepository) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.queryRepository = queryRepository;
    }

    public DocumentPage list(int page, int size) { // 方法作用：按稳定顺序或分页条件列出数据。
        if (page < 0) {
            throw new DocumentException(
                    "DOCUMENT_PAGE_INVALID", "Page must be greater than or equal to 0", HttpStatus.BAD_REQUEST);
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new DocumentException(
                    "DOCUMENT_PAGE_SIZE_INVALID",
                    "Page size must be between 1 and " + MAX_PAGE_SIZE,
                    HttpStatus.BAD_REQUEST);
        }
        return queryRepository.findPage(page, size);
    }
}
