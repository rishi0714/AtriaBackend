package com.campus.platform.club;

import com.campus.platform.club.dto.ClubDto;
import com.campus.platform.club.dto.ClubResponseDto;
import com.campus.platform.club.entity.Club;
import com.campus.platform.club.mapper.ClubMapper;
import com.campus.platform.club.repository.ClubRepository;
import com.campus.platform.club.service.ClubService;
import com.campus.platform.college.entity.College;
import com.campus.platform.college.entity.CollegeDomain;
import com.campus.platform.college.service.CollegeService;
import com.campus.platform.common.enums.UserRole;
import com.campus.platform.user.entity.User;
import com.campus.platform.user.repository.UserRepository;
import com.campus.platform.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClubService")
class ClubServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMapper clubMapper;

    @Mock
    private CollegeService collegeService;

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClubService clubService;

    private UUID collegeId;
    private College college;
    private Club club;
    private ClubDto dto;

    @BeforeEach
    void setUp() {

        collegeId = UUID.randomUUID();

        CollegeDomain domain = CollegeDomain.builder()
                .domain("gitam.edu")
                .primary(true)
                .build();

        college = College.builder()
                .collegeId(collegeId)
                .name("GITAM University")
                .domains(List.of(domain))
                .build();

        domain.setCollege(college);

        dto = ClubDto.builder()
                .name("Coding Club")
                .description("We code")
                .build();

        club = Club.builder()
                .clubId(UUID.randomUUID())
                .name("Coding Club")
                .college(college)
                .build();
    }

    @Test
    @DisplayName("createClub() persists when name is unique")
    void createClubSuccess() {

        when(collegeService.findCollegeOrThrow(collegeId))
                .thenReturn(college);

        when(clubRepository.existsByCollege_CollegeIdAndName(
                collegeId,
                "Coding Club"))
                .thenReturn(false);

        when(clubMapper.toEntity(dto, college))
                .thenReturn(club);

        when(clubRepository.save(club))
                .thenReturn(club);

        ClubResponseDto response = new ClubResponseDto();

        when(clubMapper.toResponseDto(club))
                .thenReturn(response);

        assertThat(
                clubService.createClub(collegeId, dto)
        ).isEqualTo(response);

        verify(clubRepository).save(club);
    }

    @Test
    @DisplayName("createClub() throws when duplicate name exists")
    void createClubDuplicateName() {

        when(collegeService.findCollegeOrThrow(collegeId))
                .thenReturn(college);

        when(clubRepository.existsByCollege_CollegeIdAndName(
                collegeId,
                "Coding Club"))
                .thenReturn(true);

        assertThatThrownBy(
                () -> clubService.createClub(collegeId, dto)
        )
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");

        verify(clubRepository, never()).save(any());
    }

    @Test
    @DisplayName("createClub() assigns existing student as club admin")
    void createClubWithAdminSuccess() {

        dto.setManagedByEmail("admin@gitam.edu");

        User student = User.builder()
                .userId(UUID.randomUUID())
                .email("admin@gitam.edu")
                .role(UserRole.STUDENT)
                .college(college)
                .build();

        when(collegeService.findCollegeOrThrow(collegeId))
                .thenReturn(college);

        when(clubRepository.existsByCollege_CollegeIdAndName(
                collegeId,
                "Coding Club"))
                .thenReturn(false);

        when(clubRepository.existsByManagedBy_UserId(student.getUserId()))
                .thenReturn(false);

        when(userRepository.findByEmail("admin@gitam.edu"))
                .thenReturn(Optional.of(student));

        when(clubMapper.toEntity(dto, college))
                .thenReturn(club);

        when(clubRepository.save(any()))
                .thenReturn(club);

        when(clubMapper.toResponseDto(any()))
                .thenReturn(new ClubResponseDto());

        clubService.createClub(collegeId, dto);

        assertThat(student.getRole())
                .isEqualTo(UserRole.CLUB_ADMIN);
    }

    @Test
    @DisplayName("createClub() rejects email outside college domain")
    void invalidDomainRejected() {

        dto.setManagedByEmail("admin@gmail.com");

        when(collegeService.findCollegeOrThrow(collegeId))
                .thenReturn(college);

        when(clubRepository.existsByCollege_CollegeIdAndName(
                collegeId,
                "Coding Club"))
                .thenReturn(false);

        assertThatThrownBy(
                () -> clubService.createClub(collegeId, dto)
        )
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    @DisplayName("createClub() rejects platform owner")
    void higherRoleCannotBecomeClubAdmin() {

        dto.setManagedByEmail("owner@gitam.edu");

        User platformOwner = User.builder()
                .userId(UUID.randomUUID())
                .email("owner@gitam.edu")
                .role(UserRole.PLATFORM_OWNER)
                .college(college)
                .build();

        when(collegeService.findCollegeOrThrow(collegeId))
                .thenReturn(college);

        when(clubRepository.existsByCollege_CollegeIdAndName(
                collegeId,
                "Coding Club"))
                .thenReturn(false);

        when(userRepository.findByEmail("owner@gitam.edu"))
                .thenReturn(Optional.of(platformOwner));

        assertThatThrownBy(
                () -> clubService.createClub(collegeId, dto)
        )
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("higher role");
    }

    @Test
    @DisplayName("createClub() rejects user already managing another club")
    void userAlreadyManagingClub() {

        dto.setManagedByEmail("admin@gitam.edu");

        User student = User.builder()
                .userId(UUID.randomUUID())
                .email("admin@gitam.edu")
                .role(UserRole.STUDENT)
                .college(college)
                .build();

        when(collegeService.findCollegeOrThrow(collegeId))
                .thenReturn(college);

        when(clubRepository.existsByCollege_CollegeIdAndName(
                collegeId,
                "Coding Club"))
                .thenReturn(false);

        when(userRepository.findByEmail("admin@gitam.edu"))
                .thenReturn(Optional.of(student));

        when(clubRepository.existsByManagedBy_UserId(
                student.getUserId()))
                .thenReturn(true);

        assertThatThrownBy(
                () -> clubService.createClub(collegeId, dto)
        )
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already managing");
    }
}