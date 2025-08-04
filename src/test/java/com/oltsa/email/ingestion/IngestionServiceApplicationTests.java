package com.oltsa.email.ingestion;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IngestionServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void statusEndpoint_ShouldReturnIdleState_BeforeAnyIngestion() throws Exception {
        // Test Case 1: Service is idle, has never run.
        mockMvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingestionRunning").value(false))
                .andExpect(jsonPath("$.messagesProcessed").value(0))
                .andExpect(jsonPath("$.validSenderMessages").value(0));
    }

    @Test
    @Order(2)
    void fullIngestionFlow_ShouldProcessFileAndReturnCorrectMetrics() throws Exception {
        ClassPathResource resource = new ClassPathResource("test-emails.tar.gz");

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test-emails.tar.gz",
                "application/gzip",
                resource.getInputStream()
        );

        mockMvc.perform(multipart("/start").file(multipartFile))
                .andExpect(status().isAccepted());

        // Test Case 3: Ingestion has completed.
        // We assume the test file 'test-emails.tar.gz' contains 90 files, all of which are valid.
        mockMvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingestionRunning").value(false))
                .andExpect(jsonPath("$.messagesProcessed").value(90))
                .andExpect(jsonPath("$.validSenderMessages").value(90));

        // Test the /top-senders endpoint to ensure it's consistent.
        mockMvc.perform(get("/top-senders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].count").value(13))
                .andExpect(jsonPath("$[0].email").value("sender1@example.com"))
                .andExpect(jsonPath("$[1].count").value(12))
                .andExpect(jsonPath("$[1].email").value("sender2@example.com"))
                .andExpect(jsonPath("$[2].count").value(11))
                .andExpect(jsonPath("$[2].email").value("sender3@example.com"));
    }
}