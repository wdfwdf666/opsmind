package com.opsmind.agent.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentRunControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldWaitForApprovalAndCreateOneTicketAfterRepeatedApproval() throws Exception {
        String startResponse = mockMvc.perform(post("/api/v1/agent/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"数据库连接池测试告警，请创建工单","taskType":"CREATE_TICKET"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_APPROVAL"))
                .andExpect(jsonPath("$.data.approval.status").value("PENDING"))
                .andExpect(jsonPath("$.data.approval.toolRisk").value("WRITE_REQUIRES_APPROVAL"))
                .andExpect(jsonPath("$.data.steps.length()").value(4))
                .andReturn().getResponse().getContentAsString();
        String runId = JsonPath.read(startResponse, "$.data.runId");

        String firstApproval = approve(runId, "APPROVE", "同意创建")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.refused").value(false))
                .andExpect(jsonPath("$.data.approval.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.ticket.status").value("OPEN"))
                .andExpect(jsonPath("$.data.steps.length()").value(7))
                .andReturn().getResponse().getContentAsString();
        String secondApproval = approve(runId, "APPROVE", "重复请求")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steps.length()").value(7))
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<String>read(secondApproval, "$.data.ticket.ticketId"))
                .isEqualTo(JsonPath.read(firstApproval, "$.data.ticket.ticketId"));
    }

    @Test
    void shouldRejectPendingTicketWithoutCreatingIt() throws Exception {
        String startResponse = mockMvc.perform(post("/api/v1/agent/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"偶发连接升高，请生成工单草稿","taskType":"CREATE_TICKET"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String runId = JsonPath.read(startResponse, "$.data.runId");

        approve(runId, "REJECT", "继续观察")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.approval.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.ticket").doesNotExist())
                .andExpect(jsonPath("$.data.steps.length()").value(5));
    }

    private org.springframework.test.web.servlet.ResultActions approve(
            String runId, String decision, String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/agent/runs/{runId}/approval", runId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"" + decision + "\",\"reason\":\"" + reason + "\"}"));
    }
}
