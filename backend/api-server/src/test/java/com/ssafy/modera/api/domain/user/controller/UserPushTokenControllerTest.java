package com.ssafy.modera.api.domain.user.controller;

import com.ssafy.modera.api.domain.user.dto.request.PushTokenRegisterRequest;
import com.ssafy.modera.api.domain.user.dto.response.PushTokenRegistrationResponse;
import com.ssafy.modera.api.domain.user.service.UserPushTokenCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class UserPushTokenControllerTest {

    private UserPushTokenCommandService service;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = mock(UserPushTokenCommandService.class);
        mockMvc = standaloneSetup(new UserPushTokenController(service)).build();
    }

    @Test
    void registersOrUpdatesPushToken() throws Exception {
        when(service.register(any(), any()))
                .thenReturn(new PushTokenRegistrationResponse("device-1"));

        mockMvc.perform(put("/api/v1/user/devices/push-token")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new PushTokenRegisterRequest("device-1", "fcm-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("U203"))
                .andExpect(jsonPath("$.data.deviceId").value("device-1"))
                .andExpect(jsonPath("$.data.fcmToken").doesNotExist());
    }

    @Test
    void deletesPushTokenByDevice() throws Exception {
        mockMvc.perform(delete("/api/v1/user/devices/device-1/push-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("U204"));

        verify(service).delete(null, "device-1");
    }
}
