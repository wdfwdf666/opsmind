/**
 * RAG（检索增强生成）主流程。
 * <p>基本顺序是：检索候选 → 重排 → 判断证据是否足够 → 拼装上下文 → 生成答案 → 返回引用。
 * 生成模型通过 AnswerGenerationProvider 接口接入。</p>
 */
package com.opsmind.rag.application;
