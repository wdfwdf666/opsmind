package com.opsmind.knowledge.document.api;

import com.opsmind.common.api.ApiResponse;
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

    public SemanticSearchController(SemanticSearchService searchService) {
        this.searchService = searchService;
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
}
