# EVAL-002：BM25 + Vector + RRF 混合检索

## 评测目标

验证词法召回能否补偿轻量向量模型对语义改写的不足，并使用与 EVAL-001 完全相同的数据集和指标进行可追溯对照。

## 实现口径

- 日期：2026-10-10
- 数据集：`opsmind-retrieval-v1`
- 语料：`evaluation/corpus/opsmind-operations.md`
- 候选：同一文档的 3 个 Child Chunk，共 6 条查询
- Dense 路：`local-feature-hashing-v1` 256 维，PostgreSQL pgvector 余弦 Top-K
- Lexical 路：标题路径 + Child 正文，中文字符 unigram/bigram 分词，标准 BM25（k1=1.2，b=0.75）
- 融合：RRF，k=60；候选池为 `max(20, topK × 4)`，并受全局 maxTopK=50 限制
- 延迟：从查询向量生成开始，到双路召回、RRF 和 Parent 上下文返回结束；不包含 HTTP 序列化

## 对照结果

| 策略 | Recall@1 | MRR@1 | HitRate@1 | Recall@3 | MRR@3 | HitRate@3 | P50 | P95 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Local Vector | 0.833 | 0.833 | 0.833 | 1.000 | 0.889 | 1.000 | 1.89 ms | 26.73 ms |
| Local Vector + BM25 + RRF | 1.000 | 1.000 | 1.000 | 1.000 | 1.000 | 1.000 | 2.81 ms | 13.92 ms |

## 问题与效果

EVAL-001 中的失败用例 `rollback-verification`：

> 回滚完成后如何确认核心功能正常？

本地向量策略把“发布回滚”排在第 3。混合检索利用查询与标题中的“回滚”词法证据，通过 RRF 将正确章节提升到第 1，使当前小样本的 Top-1 三项指标从 0.833 提升到 1.000，即提升 16.7 个百分点。

本次 P95 数值下降主要来自首次请求预热、样本极小和运行抖动，不能据此声称混合检索比单路向量更快；P50 从 1.89 ms 增至 2.81 ms，更符合增加一路召回的预期。

## 工程边界

- 当前 BM25 在请求时读取限定范围内的全部 Child Chunk 并在 JVM 计算，目的是在不引入新基础设施前验证分词、排序和融合契约。
- 生产扩展应将 BM25 迁移到 Elasticsearch/OpenSearch 倒排索引，使双路召回可并行并避免全量读取。
- RRF 只使用排名，不直接混合余弦分数和 BM25 分数，避免不同分数空间不可比。
- 只有 3 个候选和 6 条查询，结果不能外推为生产准确率或稳定性能。
- 项目默认 Qwen 配置仍保留；本轮未把本地评测语料发送到外部 Qwen API，因此报告只记录可在本机复现的 Local Vector 对照。

## 复现请求

```json
{
  "dataset": "opsmind-retrieval-v1",
  "documentId": "替换为上传后返回的文档 UUID",
  "strategy": "HYBRID"
}
```

调用 `POST /api/v1/evaluations/retrieval/run`。将 `strategy` 改为 `VECTOR` 可获得同口径向量基线。
