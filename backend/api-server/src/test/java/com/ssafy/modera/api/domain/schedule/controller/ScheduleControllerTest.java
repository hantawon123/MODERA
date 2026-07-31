package com.ssafy.modera.api.domain.schedule.controller;

import com.ssafy.modera.api.domain.schedule.service.ScheduleCommandService;
import com.ssafy.modera.api.domain.schedule.service.ScheduleQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ScheduleControllerTest {

    private ScheduleQueryService queryService;
    private ScheduleCommandService commandService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        queryService = mock(ScheduleQueryService.class);
        commandService = mock(ScheduleCommandService.class);
        mockMvc = standaloneSetup(new ScheduleController(queryService, commandService)).build();
    }

    @Test
    void exposesEveryScheduleEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/schedules"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("S201"));
        mockMvc.perform(delete("/api/v1/schedules/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("S202"));
        mockMvc.perform(put("/api/v1/schedules/1/calendar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calendared\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("S203"));
    }

    @Test
    void rejectsMissingCalendarFlagBeforeServiceInvocation() throws Exception {
        mockMvc.perform(put("/api/v1/schedules/1/calendar")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(queryService, commandService);
    }
}
