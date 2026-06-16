package com.campus.platform.college;

import com.campus.platform.college.controller.CollegeController;
import com.campus.platform.college.dto.*;
import com.campus.platform.college.service.CollegeService;
import com.campus.platform.security.jwt.JwtAuthenticationFilter;
import com.campus.platform.security.jwt.JwtTokenProvider;
import com.campus.platform.security.oauth2.CustomOAuth2UserService;
import com.campus.platform.security.oauth2.CustomOidcUserService;
import com.campus.platform.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.campus.platform.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CollegeController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CollegeController")
class CollegeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CollegeService collegeService;

    // Security beans required by SecurityConfig
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private CustomOidcUserService customOidcUserService;

    @MockBean
    private OAuth2AuthenticationSuccessHandler successHandler;

    @MockBean
    private OAuth2AuthenticationFailureHandler failureHandler;

    @Test
    @DisplayName("POST /setup returns 201")
    void setupCollegeReturns201() throws Exception {

        CollegeSetupDto dto = CollegeSetupDto.builder()
                .name("GITAM University")
                .domain("gitam.edu")
                .admins(List.of(
                        CollegeSetupDto.Admin.builder()
                                .email("admin@gitam.edu")
                                .fullName("Admin User")
                                .build()
                ))
                .build();

        CollegeSetupResponseDto response =
                CollegeSetupResponseDto.builder()
                        .collegeId(UUID.randomUUID())
                        .name("GITAM University")
                        .domain("gitam.edu")
                        .build();

        when(collegeService.setupCollege(any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/platform/colleges/setup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name")
                        .value("GITAM University"));
    }

    @Test
    @DisplayName("POST /colleges returns 201")
    void createCollegeReturns201() throws Exception {

        CollegeDto dto = CollegeDto.builder()
                .name("Atria")
                .logoUrl("logo.png")
                .build();

        CollegeResponseDto response =
                CollegeResponseDto.builder()
                        .collegeId(UUID.randomUUID())
                        .name("Atria")
                        .logoUrl("logo.png")
                        .build();

        when(collegeService.createCollege(any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/platform/colleges")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name")
                        .value("Atria"));
    }

    @Test
    @DisplayName("GET /colleges returns list")
    void getAllCollegesReturns200() throws Exception {

        when(collegeService.getAllColleges())
                .thenReturn(List.of(
                        CollegeResponseDto.builder()
                                .collegeId(UUID.randomUUID())
                                .name("Atria")
                                .build()
                ));

        mockMvc.perform(get("/api/platform/colleges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name")
                        .value("Atria"));
    }

    @Test
    @DisplayName("GET /colleges/{id} returns college")
    void getCollegeByIdReturns200() throws Exception {

        UUID collegeId = UUID.randomUUID();

        when(collegeService.getCollegeById(collegeId))
                .thenReturn(
                        CollegeResponseDto.builder()
                                .collegeId(collegeId)
                                .name("Atria")
                                .build()
                );

        mockMvc.perform(get("/api/platform/colleges/{collegeId}", collegeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Atria"));
    }

    @Test
    @DisplayName("PUT /colleges/{id} updates college")
    void updateCollegeReturns200() throws Exception {

        UUID collegeId = UUID.randomUUID();

        CollegeDto dto = CollegeDto.builder()
                .name("Updated College")
                .logoUrl("logo.png")
                .build();

        when(collegeService.updateCollege(any(), any()))
                .thenReturn(
                        CollegeResponseDto.builder()
                                .collegeId(collegeId)
                                .name("Updated College")
                                .logoUrl("logo.png")
                                .build()
                );

        mockMvc.perform(put("/api/platform/colleges/{collegeId}", collegeId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Updated College"));
    }

    @Test
    @DisplayName("PATCH status updates college")
    void setActiveStatusReturns200() throws Exception {

        UUID collegeId = UUID.randomUUID();

        when(collegeService.setActiveStatus(collegeId, false))
                .thenReturn(
                        CollegeResponseDto.builder()
                                .collegeId(collegeId)
                                .name("Atria")
                                .isActive(false)
                                .build()
                );

        mockMvc.perform(
                        patch("/api/platform/colleges/{collegeId}/status", collegeId)
                                .with(csrf())
                                .param("isActive", "false"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET domains returns list")
    void getDomainsReturns200() throws Exception {

        UUID collegeId = UUID.randomUUID();

        when(collegeService.getDomainsForCollege(collegeId))
                .thenReturn(List.of(
                        CollegeDomainResponseDto.builder()
                                .id(UUID.randomUUID())
                                .domain("gitam.edu")
                                .primary(true)
                                .build()
                ));

        mockMvc.perform(get("/api/platform/colleges/{collegeId}/domains", collegeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].domain")
                        .value("gitam.edu"));
    }
}