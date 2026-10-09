package com.opsmind.knowledge.document.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "opsmind.document.storage-location=target/test-api-documents")
@AutoConfigureMockMvc
class DocumentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldUploadDocumentAndDetectDuplicate() throws Exception {
        MockMultipartFile first = new MockMultipartFile(
                "file", "incident-guide.txt", MediaType.TEXT_PLAIN_VALUE, "unique api content".getBytes());
        MockMultipartFile duplicate = new MockMultipartFile(
                "file", "renamed-guide.txt", MediaType.TEXT_PLAIN_VALUE, "unique api content".getBytes());

        mockMvc.perform(multipart("/api/v1/documents").file(first))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.filename").value("incident-guide.txt"))
                .andExpect(jsonPath("$.data.documentStatus").value("UPLOADED"))
                .andExpect(jsonPath("$.data.taskStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.duplicate").value(false));

        mockMvc.perform(multipart("/api/v1/documents").file(duplicate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.filename").value("incident-guide.txt"))
                .andExpect(jsonPath("$.data.duplicate").value(true));
    }

    @Test
    void shouldReturnStructuredErrorForUnsupportedFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.exe", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/documents").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("DOCUMENT_TYPE_NOT_SUPPORTED"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldProcessAndReturnCleanedContent() throws Exception {
        String uniqueText = "restart-service-" + UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "operations.txt", MediaType.TEXT_PLAIN_VALUE,
                ("Operations\r\n\r\n\r\n" + uniqueText).getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/v1/documents").file(file))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String documentId = com.jayway.jsonpath.JsonPath.read(uploadResponse, "$.data.documentId");

        mockMvc.perform(post("/api/v1/documents/{documentId}/process", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentStatus").value("READY"))
                .andExpect(jsonPath("$.data.taskStatus").value("SUCCEEDED"));

        mockMvc.perform(get("/api/v1/documents/{documentId}/content", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.text").value("Operations\n\n" + uniqueText))
                .andExpect(jsonPath("$.data.characterCount").value(("Operations\n\n" + uniqueText).length()));
    }
}
