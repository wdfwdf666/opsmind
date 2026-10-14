package com.opsmind.system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "opsmind.agent.planner.provider=qwen",
        "opsmind.agent.planner.base-url=https://planner.example/v1",
        "opsmind.agent.planner.api-key=test-key"
})
@AutoConfigureMockMvc
class PrometheusEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposePlannerMetricsInPrometheusFormat() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(content().string(containsString("opsmind_agent_planner_circuit_state")));
    }
}
