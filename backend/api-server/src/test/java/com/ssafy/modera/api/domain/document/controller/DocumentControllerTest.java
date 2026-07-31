package com.ssafy.modera.api.domain.document.controller;

import com.ssafy.modera.api.domain.document.service.DocumentCommandService;
import com.ssafy.modera.api.domain.document.service.DocumentDeleteService;
import com.ssafy.modera.api.domain.document.service.DocumentQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class DocumentControllerTest {

    private DocumentCommandService commandService;
    private DocumentQueryService queryService;
    private DocumentDeleteService deleteService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandService = mock(DocumentCommandService.class);
        queryService = mock(DocumentQueryService.class);
        deleteService = mock(DocumentDeleteService.class);
        mockMvc = standaloneSetup(new DocumentController(
                commandService, queryService, deleteService)).build();
    }

    @Test
    void exposesEveryDocumentEndpoint() throws Exception {
        String requestId = "d95db8b7-897e-412c-8924-eef3c7bca039";

        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":\"" + requestId + "\",\"imageIds\":[1]}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("D201"));
        mockMvc.perform(get("/api/v1/documents/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("D203"));
        mockMvc.perform(get("/api/v1/documents/1/images"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("D204"));
        mockMvc.perform(post("/api/v1/documents/1/regenerate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":\"" + requestId + "\",\"imageIds\":null}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/documents/1/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":\"" + requestId + "\",\"imageIds\":[2]}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/documents/1/images/exclude")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":\"" + requestId + "\",\"imageIds\":[1]}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/documents/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("D205"));
    }

    @Test
    void rejectsEmptyDocumentMutationsBeforeServiceInvocation() throws Exception {
        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":null,\"imageIds\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/documents/1/regenerate")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/documents/1/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":null,\"imageIds\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/documents/1/images/exclude")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRequestId\":null,\"imageIds\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commandService, queryService, deleteService);
    }
}
