# EVAL-001：Qwen 与本地 Embedding 检索基线

## 评测口径

- 日期：2026-10-09
- 数据集：`opsmind-retrieval-v1`
- 语料：`evaluation/corpus/opsmind-operations.md`
- 用例：6 条中文查询，每个问题标注 1 个相关标题路径
- 候选：同一文档的 3 个 Child Chunk
- 检索：PostgreSQL pgvector 余弦 Top-K，分别统计 K=1、K=3
- 延迟：从生成查询向量开始，到 PostgreSQL 检索和 Parent 上下文返回结束；不包含 HTTP 控制器序列化
- 环境：Windows 本地开发机、Java 21、Spring Boot 4.1.1、PostgreSQL 17.11、pgvector 0.8.7

## 对照结果

| 模型 | Recall@1 | MRR@1 | HitRate@1 | Recall@3 | MRR@3 | HitRate@3 | P50 | P95 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| `qwen3.7-text-embedding`（1024 维） | 1.000 | 1.000 | 1.000 | 1.000 | 1.000 | 1.000 | 170.77 ms | 420.14 ms |
| `local-feature-hashing-v1`（256 维） | 0.833 | 0.833 | 0.833 | 1.000 | 0.889 | 1.000 | 1.64 ms | 24.02 ms |

## 失败案例

本地特征哈希模型在 `rollback-verification` 用例中把正确章节“OpsMind 运维手册 > 发布回滚”排在第 3：

> 回滚完成后如何确认核心功能正常？

该查询与原文“完成后对核心接口执行冒烟测试”词面重合较少，暴露了字符 n-gram 模型对语义改写的局限。Qwen 将该章节排在第 1。

## 结论

- Qwen 在当前 6 条语义改写查询中保持 100% Top-1 命中，优于本地词面模型的 83.3%。
- 本地模型无需网络且延迟显著更低，适合作为自动化测试和故障降级基线，但不能替代生产语义模型。
- 当前数据集只有 1 篇文档、3 个 Child Chunk、6 个问题，结果只能证明评测链路和该小样本上的差异，不能外推为生产准确率或稳定延迟。
- 下一轮应扩展到至少 100 条、多个文档，并加入专有名词、跨文档、无答案和困难负样本；随后评测 BM25、向量和 RRF 混合检索。

## 复现步骤

1. 上传并处理 `evaluation/corpus/opsmind-operations.md`，重建 Parent-Child Chunk。
2. 分别使用 Qwen 与本地 Provider 重建该文档向量。
3. 调用 `POST /api/v1/evaluations/retrieval/run`：

```json
{
  "dataset": "opsmind-retrieval-v1",
  "documentId": "替换为上传后返回的文档 UUID"
}
```

同一份 JSON 数据集固定查询、相关标题和 K 值，后续修改模型、分块或检索策略后应重新运行并保留新报告。
