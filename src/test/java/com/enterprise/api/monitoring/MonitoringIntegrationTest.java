package com.enterprise.api.monitoring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class MonitoringIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Test
    void actuatorHealth_ShouldReturnHealthStatus() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void actuatorInfo_ShouldReturnApplicationInfo() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
                .andExpect(jsonPath("$.application").exists())
                .andExpect(jsonPath("$.features").exists())
                .andExpect(jsonPath("$.monitoring").exists());
    }

    @Test
    void actuatorMetrics_ShouldReturnMetricsList() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
                .andExpect(jsonPath("$.names").exists())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    void actuatorHttpExchanges_ShouldReturnHttpExchanges() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/actuator/httpexchanges"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
                .andExpect(jsonPath("$.exchanges").exists())
                .andExpect(jsonPath("$.exchanges").isArray());
    }

    @Test
    void actuatorBeans_ShouldReturnBeansList() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
                .andExpect(jsonPath("$.contexts").exists());
    }

    @Test
    void actuatorEnv_ShouldReturnEnvironmentProperties() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
                .andExpect(jsonPath("$.activeProfiles").exists())
                .andExpect(jsonPath("$.propertySources").exists());
    }
}