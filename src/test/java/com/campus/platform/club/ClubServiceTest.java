package com.campus.platform.club;

import com.campus.platform.club.dto.ClubDto;
import com.campus.platform.club.dto.ClubResponseDto;
import com.campus.platform.club.entity.Club;
import com.campus.platform.club.mapper.ClubMapper;
import com.campus.platform.club.repository.ClubRepository;
import com.campus.platform.club.service.ClubService;
import com.campus.platform.college.entity.College;
import com.campus.platform.college.service.CollegeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClubService")
class ClubServiceTest {

    @Mock ClubRepository clubRepository;
    @Mock ClubMapper clubMapper;
    @Mock CollegeService collegeService;

    @InjectMocks ClubService clubService;

    private UUID collegeId;
    private College college;
    private Club club;
    private ClubDto dto;

    @BeforeEach
    void setUp() {
        collegeId = UUID.randomUUID();
        college = College.builder().collegeId(collegeId).name("Test College").build();
        dto = ClubDto.builder().name("Coding Club").description("We code").build();
        club = Club.builder().clubId(UUID.randomUUID()).college(college).name("Coding Club").build();
    }

    @Test
    @DisplayName("createClub() persists when name is unique within tenant")
    void createClubSuccess() {
        when(collegeService.findCollegeOrThrow(collegeId)).thenReturn(college);
        when(clubRepository.existsByCollege_CollegeIdAndName(collegeId, "Coding Club")).thenReturn(false);
        when(clubMapper.toEntity(dto, college)).thenReturn(club);
        when(clubRepository.save(club)).thenReturn(club);
        ClubResponseDto response = new ClubResponseDto();
        when(clubMapper.toResponseDto(club)).thenReturn(response);

        assertThat(clubService.createClub(collegeId, dto)).isEqualTo(response);
        verify(clubRepository).save(club);
    }

    @Test
    @DisplayName("createClub() throws 409 when name already taken in same tenant")
    void createClubDuplicateName() {
        when(collegeService.findCollegeOrThrow(collegeId)).thenReturn(college);
        when(clubRepository.existsByCollege_CollegeIdAndName(collegeId, "Coding Club")).thenReturn(true);

        assertThatThrownBy(() -> clubService.createClub(collegeId, dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");

        verify(clubRepository, never()).save(any());
    }
}
