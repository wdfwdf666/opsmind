# EVAL-003：混合召回后的 Reranker 精排

## 评测目标

验证“Dense + BM25 + RRF 召回候选，再进行 Query-Passage 精排”的端到端编排、排序结果映射和评测策略，而不是只验证供应商 HTTP 请求。

## 实现口径

- 日期：2026-10-11
- 数据集：`opsmind-retrieval-v1`
- 候选：同一文档的 3 个 Child Chunk，共 6 条查询
- 召回：Local Vector + BM25 + RRF
- 精排：`local-token-overlap-reranker-v1` 确定性测试 Provider
- 生产 Provider：`qwen3.7-text-rerank`，使用阿里云百炼独立 Rerank API
- 候选池：`max(20, topK × 4)`，受全局 `maxTopK=50` 限制

## 对照结果

| 策略 | Recall@1 | MRR@1 | HitRate@1 | Recall@3 | MRR@3 | HitRate@3 | P50 | P95 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Local Vector + BM25 + RRF | 1.000 | 1.000 | 1.000 | 1.000 | 1.000 | 1.000 | 2.81 ms | 13.92 ms |
| 上述召回 + Local Reranker | 1.000 | 1.000 | 1.000 | 1.000 | 1.000 | 1.000 | 2.57 ms | 42.06 ms |

## 结论

- RERANKED 策略的接口、候选扩大、索引映射、分数替换、Top-K 截断和评测链路均可运行。
- 当前 Hybrid 在 6 条查询上已达到 Top-1 100%，存在明显天花板，因此不能从该数据集得出 Reranker 提升准确率的结论。
- P95 上升来自首次请求预热和 6 次小样本抖动，不能作为稳定性能结论；也不能代表远程 Qwen 的网络延迟。
- 本地 Provider 是离线确定性测试替身，不是 Cross-Encoder。生产质量必须在获得数据外发授权后使用 Qwen，并在更多困难负样本上重新评测。

## 安全与工程边界

- `QWEN_RERANK_URL` 与 Embedding URL 独立配置，避免因接口协议不同拼错路径。
- 缺少 URL 或 API Key 时返回 503，不静默降级；上游错误响应体不会返回给调用方。
- Qwen 返回的候选原始索引、重复索引、分数范围和结果数量都会被校验，防止候选与分数错位。
- Rerank 分数是一次请求内部的相对相关性，不应跨请求比较或当成置信概率。

## 复现请求

```json
{
  "dataset": "opsmind-retrieval-v1",
  "documentId": "替换为上传后返回的文档 UUID",
  "strategy": "RERANKED"
}
```

调用 `POST /api/v1/evaluations/retrieval/run`。业务调试可调用 `POST /api/v1/search/reranked`，请求体与现有搜索接口一致。
