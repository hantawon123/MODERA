package com.ssafy.modera.api.domain.notification.controller;

import com.ssafy.modera.api.domain.notification.dto.PushSendResult;
import com.ssafy.modera.api.domain.notification.service.UserPushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PushTestControllerTest {

    private UserPushNotificationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(UserPushNotificationService.class);
        mockMvc = standaloneSetup(new PushTestController(service)).build();
    }

    @Test
    void sendsTestMessageOnlyToAuthenticatedUser() throws Exception {
        when(service.sendDataChanged(null, "SYNC_TEST", null))
                .thenReturn(new PushSendResult(true, 1, 1, 0));

        mockMvc.perform(post("/api/v1/user/devices/push-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("U205"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.successCount").value(1));

        verify(service).sendDataChanged(null, "SYNC_TEST", null);
    }
}
