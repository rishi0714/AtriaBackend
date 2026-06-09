package com.campus.platform.college;

import com.campus.platform.college.controller.CollegeController;
import com.campus.platform.college.dto.CollegeDto;
import com.campus.platform.college.dto.CollegeResponseDto;
import com.campus.platform.college.service.CollegeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CollegeController.class)
@DisplayName("CollegeController (WebMvc slice)")
class CollegeControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean CollegeService collegeService;

    private static final String BASE = "/api/superadmin/colleges";

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("POST /colleges → 201 with response body")
    void createCollege() throws Exception {
        CollegeDto dto = CollegeDto.builder()
                .name("Sreenidhi Institute")
                .domain("sreenidhi.edu.in")
                .isActive(true)
                .build();

        CollegeResponseDto response = CollegeResponseDto.builder()
                .collegeId(UUID.randomUUID())
                .name("Sreenidhi Institute")
                .domain("sreenidhi.edu.in")
                .isActive(true)
                .build();

        when(collegeService.createCollege(any())).thenReturn(response);

        mockMvc.perform(post(BASE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.domain").value("sreenidhi.edu.in"))
                .andExpect(jsonPath("$.collegeId").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("GET /colleges → 200 with list")
    void getAllColleges() throws Exception {
        when(collegeService.getAllColleges()).thenReturn(List.of(
                CollegeResponseDto.builder()
                        .collegeId(UUID.randomUUID())
                        .name("Test College")
                        .domain("test.edu")
                        .isActive(true)
                        .build()
        ));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test College"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")  // wrong role
    @DisplayName("POST /colleges → 403 when not SUPER_ADMIN")
    void createCollegeForbiddenForStudent() throws Exception {
        mockMvc.perform(post(BASE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("POST /colleges → 422 when domain is blank")
    void createCollegeValidationFails() throws Exception {
        CollegeDto bad = CollegeDto.builder().name("").domain("").build();

        mockMvc.perform(post(BASE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isUnprocessableEntity());
    }
}
