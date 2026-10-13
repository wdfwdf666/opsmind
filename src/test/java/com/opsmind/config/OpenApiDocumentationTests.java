package com.opsmind.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGenerateOpenApiDocumentForBusinessEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("OpsMind API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths['/api/v1/documents'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/documents'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/documents'].post.requestBody.content['multipart/form-data']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/documents/{documentId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/documents/{documentId}/process'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/documents/{documentId}/content'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/documents/{documentId}/chunks/rebuild'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/documents/{documentId}/chunks'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/documents/{documentId}/embeddings/rebuild'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/search'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/search/hybrid'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/search/reranked'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/rag/answer'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/evaluations/retrieval/run'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/evaluations/answers/run'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/evaluations/agent-routing/run'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/agent/runs'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/agent/runs/{runId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/agent/runs/{runId}/approval'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/info'].get").exists());
    }

    @Test
    void shouldServeSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }
}
