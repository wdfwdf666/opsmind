package com.opsmind.knowledge.document.api;

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

@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Knowledge Search", description = "Search indexed child chunks and expand parent context")
public class SemanticSearchController {

    private final SemanticSearchService searchService;
    private final HybridSearchService hybridSearchService;
    private final RerankedSearchService rerankedSearchService;

    public SemanticSearchController(
            SemanticSearchService searchService,
            HybridSearchService hybridSearchService,
            RerankedSearchService rerankedSearchService) {
        this.searchService = searchService;
        this.hybridSearchService = hybridSearchService;
        this.rerankedSearchService = rerankedSearchService;
    }

    @PostMapping
    @Operation(
            summary = "Search knowledge chunks by vector similarity",
            description = "Embeds the query, ranks indexed child chunks by cosine similarity and returns parent context.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Ranked semantic search results"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid query or topK", content = @Content)
    })
    public ApiResponse<SemanticSearchResponse> search(@RequestBody SemanticSearchRequest request) {
        return ApiResponse.success(SemanticSearchResponse.from(
                searchService.search(request.query(), request.topK(), request.documentId())));
    }

    @PostMapping("/hybrid")
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
    public ApiResponse<SemanticSearchResponse> hybridSearch(@RequestBody SemanticSearchRequest request) {
        return ApiResponse.success(SemanticSearchResponse.from(
                hybridSearchService.search(request.query(), request.topK(), request.documentId())));
    }

    @PostMapping("/reranked")
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
    public ApiResponse<SemanticSearchResponse> rerankedSearch(@RequestBody SemanticSearchRequest request) {
        return ApiResponse.success(SemanticSearchResponse.from(
                rerankedSearchService.search(request.query(), request.topK(), request.documentId())));
    }
}
