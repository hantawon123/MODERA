package com.ssafy.modera.api.domain.storage.controller;

import com.ssafy.modera.api.domain.storage.service.StorageWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class StorageWebhookControllerTest {

    private StorageWebhookService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(StorageWebhookService.class);
        mockMvc = standaloneSetup(new StorageWebhookController(service, "secret")).build();
    }

    @Test
    void acceptsEitherSupportedTokenHeader() throws Exception {
        mockMvc.perform(post("/internal/storage/events")
                        .header("X-Webhook-Token", "secret")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/internal/storage/events")
                        .header("Authorization", "Bearer secret")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/internal/storage/events")
                        .header("Authorization", "secret")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        verify(service, org.mockito.Mockito.times(3)).handle(any());
    }

    @Test
    void rejectsMissingAndWrongTokens() throws Exception {
        mockMvc.perform(post("/internal/storage/events")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/internal/storage/events")
                        .header("X-Webhook-Token", "wrong")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
