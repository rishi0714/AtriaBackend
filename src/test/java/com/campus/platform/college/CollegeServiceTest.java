package com.campus.platform.college;

import com.campus.platform.college.dto.CollegeDto;
import com.campus.platform.college.dto.CollegeResponseDto;
import com.campus.platform.college.entity.College;
import com.campus.platform.college.mapper.CollegeMapper;
import com.campus.platform.college.repository.CollegeRepository;
import com.campus.platform.college.service.CollegeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollegeService")
class CollegeServiceTest {

    @Mock CollegeRepository collegeRepository;
    @Mock CollegeMapper collegeMapper;

    @InjectMocks CollegeService collegeService;

    private CollegeDto dto;
    private College college;
    private CollegeResponseDto responseDto;

    @BeforeEach
    void setUp() {
        dto = CollegeDto.builder()
                .name("Sreenidhi Institute")
                .domain("sreenidhi.edu.in")
                .isActive(true)
                .build();

        college = College.builder()
                .collegeId(UUID.randomUUID())
                .name("Sreenidhi Institute")
                .domain("sreenidhi.edu.in")
                .isActive(true)
                .build();

        responseDto = CollegeResponseDto.builder()
                .collegeId(college.getCollegeId())
                .name(college.getName())
                .domain(college.getDomain())
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("createCollege()")
    class CreateCollege {

        @Test
        @DisplayName("returns response DTO when domain is unique")
        void success() {
            when(collegeRepository.existsByDomain(anyString())).thenReturn(false);
            when(collegeMapper.toEntity(dto)).thenReturn(college);
            when(collegeRepository.save(college)).thenReturn(college);
            when(collegeMapper.toResponseDto(college)).thenReturn(responseDto);

            CollegeResponseDto result = collegeService.createCollege(dto);

            assertThat(result.getDomain()).isEqualTo("sreenidhi.edu.in");
            verify(collegeRepository).save(college);
        }

        @Test
        @DisplayName("throws 409 CONFLICT when domain already exists")
        void duplicateDomain() {
            when(collegeRepository.existsByDomain(anyString())).thenReturn(true);

            assertThatThrownBy(() -> collegeService.createCollege(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("already exists");

            verify(collegeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("setActiveStatus()")
    class SetActiveStatus {

        @Test
        @DisplayName("deactivates an existing college")
        void deactivate() {
            when(collegeRepository.findById(college.getCollegeId()))
                    .thenReturn(Optional.of(college));
            when(collegeRepository.save(college)).thenReturn(college);
            when(collegeMapper.toResponseDto(college)).thenReturn(responseDto);

            collegeService.setActiveStatus(college.getCollegeId(), false);

            assertThat(college.isActive()).isFalse();
        }

        @Test
        @DisplayName("throws 404 when college not found")
        void notFound() {
            UUID missing = UUID.randomUUID();
            when(collegeRepository.findById(missing)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> collegeService.setActiveStatus(missing, true))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Test
    @DisplayName("resolveByDomain() throws 403 when domain is inactive")
    void resolveByDomainInactive() {
        when(collegeRepository.findByDomainAndIsActiveTrue("unknown.edu"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> collegeService.resolveByDomain("unknown.edu"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No active college");
    }
}
