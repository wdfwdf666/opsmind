# EVAL-004：带引用 RAG 回答工程验证

## 目标

验证 `混合召回 → Qwen Reranker 接口边界 → Parent 上下文组装 → Chat 生成 → 引用映射` 的完整工程链路，并明确区分“引用格式有效”和“答案事实忠实”两类能力。

## 环境与口径

- 日期：2026-10-11
- 数据库：PostgreSQL 17.11 + pgvector 0.8.7
- 文档：`feat007.md`，文档 ID `79fc1fcf-93f1-44a0-bc86-95cac599f082`
- 端到端验证 Provider：本地 256 维特征哈希 Embedding、本地 Token Overlap Reranker、本地确定性抽取生成器
- Qwen Chat：使用 Mock HTTP 服务验证 OpenAI 兼容协议、Bearer 鉴权、请求字段、Token 用量解析和错误脱敏；未向真实外部服务发送本地文档

## 验证用例与结果

请求：

```json
{
  "query": "回滚完成后如何确认核心功能正常？",
  "documentId": "79fc1fcf-93f1-44a0-bc86-95cac599f082",
  "topK": 3
}
```

真实本地接口 `POST /api/v1/rag/answer` 返回：

- `refused=false`；
- 回答引用 `[S1]`；
- `[S1]` 映射到 `feat007.md / OpsMind 运维手册 > 发布回滚`；
- 返回 Parent 上下文，引用同时保留命中的 Child Chunk ID、Parent ID、文档 ID、标题路径和精排分数；
- 本次本地确定性链路不调用远程模型，因此 Token 用量为 0。

自动化验证覆盖：

1. 只返回回答中实际使用的引用，未使用证据不会进入 `citations`。
2. 零证据时返回固定中文拒答，不调用生成 Provider。
3. 模型未输出引用或输出不存在的来源编号时返回 `502 ANSWER_PROVIDER_INVALID_RESPONSE`。
4. Qwen 请求发送到 `{QWEN_CHAT_BASE_URL}/chat/completions`，包含模型、System/User 消息、`max_tokens`、温度和 `enable_thinking=false`。
5. System Prompt 将 evidence JSON 标记为不可信数据，禁止执行文档内指令、改变角色或使用模型记忆补造内部事实。
6. 缺少配置返回 503；上游 HTTP/网络异常转换为脱敏 502，不向客户端透传响应正文或密钥。

最终完整测试结果：54 个测试，0 失败，0 错误。

## 结论

FEAT-013 已证明回答 API、证据预算、Parent 去重、来源编号、引用回映、零证据拒答及 Qwen 协议边界可用。它尚不能证明回答质量达到生产要求：引用编号存在只代表格式和来源范围合法，不代表每一句结论都由对应原文蕴含。

## 限制与下一步

- 当前拒答条件是“没有任何检索证据”，没有把不同 Reranker 的分数当作跨请求可比较阈值；需要在扩充正负样本后校准低相关度拒答策略。
- 未执行真实 Qwen Chat 调用，因为这会把本地文档片段发送到外部模型服务；须在明确授权可外发的数据集后单独验证质量、延迟和费用。
- 下一阶段建立固定问答集，评测 Answer Correctness、Faithfulness、Citation Precision/Recall 和拒答准确率；再把 Token 用量写入 Micrometer 指标。
