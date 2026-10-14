/**
 * 知识库文档模块的用例编排层。
 * <p>上传、解析、清洗、父子分块、向量化、混合检索和重排都在这里串联。
 * 接口（如 EmbeddingProvider）描述需要的能力，具体实现放在 infrastructure，
 * 这样替换模型或存储时不会改动核心流程。</p>
 */
package com.opsmind.knowledge.document.application;
