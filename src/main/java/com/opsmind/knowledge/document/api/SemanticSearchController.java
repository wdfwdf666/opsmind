package com.opsmind.knowledge.document.api; // 包作用：接口层，接收 HTTP/JSON 数据并转换成应用层调用。

import com.opsmind.common.api.ApiResponse;
import com.opsmind.knowledge.document.application.HybridSearchService;
import com.opsmind.knowledge.document.application.RerankedSearchService;
import com.opsmind.knowledge.document.application.SemanticSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 三种检索策略的 HTTP 入口：向量检索、混合检索、混合检索后重排。
 * <p>把三种接口并列保留，方便学习、调试和离线评测比较效果。</p>
 */
@RestController // 注解作用：把类注册为 REST 控制器，返回对象会自动转换成 JSON。
@RequestMapping("/api/v1/search") // 注解作用：声明该控制器所有接口共用的 URL 前缀。
@Tag(name = "Knowledge Search", description = "Search indexed child chunks and expand parent context")
public class SemanticSearchController { // 类型作用：HTTP 控制器，只负责收参、调用 Service 和返回 JSON。

    private final SemanticSearchService searchService;
    private final HybridSearchService hybridSearchService;
    private final RerankedSearchService rerankedSearchService;

    public SemanticSearchController(
            SemanticSearchService searchService,
            HybridSearchService hybridSearchService,
            RerankedSearchService rerankedSearchService) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.searchService = searchService;
        this.hybridSearchService = hybridSearchService;
        this.rerankedSearchService = rerankedSearchService;
    }

    @PostMapping // 注解作用：把 HTTP POST 请求映射到下面的方法。
    @Operation(
            summary = "Search knowledge chunks by vector similarity",
            description = "Embeds the query, ranks indexed child chunks by cosine similarity and returns parent context.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Ranked semantic search results"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid query or topK", content = @Content)
    })
    public ApiResponse<SemanticSearchResponse> search(@RequestBody SemanticSearchRequest request) { // 方法作用：执行检索或相关性排序并返回有序结果。
        // Controller 不实现检索算法，只把请求交给对应 Service 并转换响应。
        return ApiResponse.success(SemanticSearchResponse.from(
                searchService.search(request.query(), request.topK(), request.documentId())));
    }

    @PostMapping("/hybrid") // 注解作用：把 HTTP POST 请求映射到下面的方法。
    @Operation(
            summary = "Search knowledge chunks with hybrid retrieval",
            description = "Combines pgvector dense retrieval and Chinese n-gram BM25 with Reciprocal Rank Fusion. "
                    + "The returned score is an RRF score, not cosine similarity.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "RRF-ranked hybrid search results"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid query or topK", content = @Content)
    })
    public ApiResponse<SemanticSearchResponse> hybridSearch(@RequestBody SemanticSearchRequest request) { // 方法作用：同时执行向量与 BM25 检索，再用 RRF 融合名次。
        return ApiResponse.success(SemanticSearchResponse.from(
                hybridSearchService.search(request.query(), request.topK(), request.documentId())));
    }

    @PostMapping("/reranked") // 注解作用：把 HTTP POST 请求映射到下面的方法。
    @Operation(
            summary = "Search and rerank knowledge chunks",
            description = "Runs hybrid retrieval, then uses the configured reranker to score query-passage relevance. "
                    + "The returned score is the rerank relevance score and must not be compared across requests.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Reranked search results"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid query or topK", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502", description = "Reranking provider failed", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503", description = "Reranking provider is not configured", content = @Content)
    })
    public ApiResponse<SemanticSearchResponse> rerankedSearch(@RequestBody SemanticSearchRequest request) { // 方法作用：先召回候选，再用重排模型选出最相关结果。
        return ApiResponse.success(SemanticSearchResponse.from(
                rerankedSearchService.search(request.query(), request.topK(), request.documentId())));
    }
}
