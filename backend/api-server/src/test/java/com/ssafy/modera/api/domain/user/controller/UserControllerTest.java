package com.ssafy.modera.api.domain.user.controller;

import com.ssafy.modera.api.domain.user.dto.response.UserInfoResponse;
import com.ssafy.modera.api.domain.user.service.UserQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class UserControllerTest {

    private UserQueryService userQueryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userQueryService = mock(UserQueryService.class);
        mockMvc = standaloneSetup(new UserController(userQueryService)).build();
    }

    @Test
    void returnsMyInfoInSpecificationShape() throws Exception {
        when(userQueryService.getMyInfo(null))
                .thenReturn(new UserInfoResponse(
                        1, "newUser123", "user@example.com", true, true));

        mockMvc.perform(get("/api/v1/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value("U201"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.loginId").value("newUser123"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.notification").value(true))
                .andExpect(jsonPath("$.data.backgroundAnalysis").value(true));

        verify(userQueryService).getMyInfo(null);
    }
}
