package com.campus.platform.college;

import com.campus.platform.college.dto.*;
import com.campus.platform.college.entity.College;
import com.campus.platform.college.entity.CollegeDomain;
import com.campus.platform.college.mapper.CollegeMapper;
import com.campus.platform.college.repository.CollegeDomainRepository;
import com.campus.platform.college.repository.CollegeRepository;
import com.campus.platform.college.service.CollegeService;
import com.campus.platform.common.enums.UserRole;
import com.campus.platform.user.dto.UserResponseDto;
import com.campus.platform.user.entity.User;
import com.campus.platform.user.mapper.UserMapper;
import com.campus.platform.user.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollegeService")
class CollegeServiceTest {
    @Mock
    private CollegeRepository collegeRepository;

    @Mock
    private CollegeDomainRepository collegeDomainRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CollegeMapper collegeMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CollegeService collegeService;

    private CollegeSetupDto setupDto;

    @BeforeEach
    void setUp() {

        setupDto = CollegeSetupDto.builder()
                .name("GITAM University")
                .domain("gitam.edu")
                .admins(List.of(
                        CollegeSetupDto.Admin.builder()
                                .email("admin@gitam.edu")
                                .fullName("Admin User")
                                .build()
                ))
                .build();
    }

    @Test
    @DisplayName("setupCollege() creates college successfully")
    void setupCollegeSuccess() {

        when(collegeRepository.existsByNameIgnoreCase(anyString()))
                .thenReturn(false);

        when(collegeDomainRepository.existsByDomain(anyString()))
                .thenReturn(false);

        College savedCollege = College.builder()
                .collegeId(UUID.randomUUID())
                .name("GITAM University")
                .build();

        when(collegeRepository.save(any()))
                .thenReturn(savedCollege);

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.empty());

        when(userRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(userMapper.toResponseDto(any(User.class)))
                .thenReturn(
                        UserResponseDto.builder()
                                .email("admin@gitam.edu")
                                .fullName("Admin User")
                                .role(UserRole.COLLEGE_ADMIN)
                                .build()
                );

        CollegeSetupResponseDto response =
                collegeService.setupCollege(setupDto);

        assertThat(response).isNotNull();

        verify(collegeRepository).save(any());
        verify(collegeDomainRepository).save(any());
        verify(userRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("throws 409 when college already exists")
    void duplicateCollege() {

        when(collegeRepository.existsByNameIgnoreCase(anyString()))
                .thenReturn(true);

        assertThatThrownBy(() ->
                collegeService.setupCollege(setupDto))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("throws 409 when domain already exists")
    void duplicateDomain() {

        when(collegeRepository.existsByNameIgnoreCase(anyString()))
                .thenReturn(false);

        when(collegeDomainRepository.existsByDomain(anyString()))
                .thenReturn(true);

        assertThatThrownBy(() ->
                collegeService.setupCollege(setupDto))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("throws 400 when admin email domain mismatches")
    void emailDomainMismatch() {

        setupDto.getAdmins().get(0).setEmail("admin@gmail.com");

        when(collegeRepository.existsByNameIgnoreCase(anyString()))
                .thenReturn(false);

        when(collegeDomainRepository.existsByDomain(anyString()))
                .thenReturn(false);

        assertThatThrownBy(() ->
                collegeService.setupCollege(setupDto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("match");
    }

    @Test
    @DisplayName("throws 409 when admin belongs to another college")
    void adminAlreadyAssignedElsewhere() {

        College otherCollege = College.builder()
                .collegeId(UUID.randomUUID())
                .build();

        User existingAdmin = User.builder()
                .email("admin@gitam.edu")
                .role(UserRole.COLLEGE_ADMIN)
                .college(otherCollege)
                .build();

        when(collegeRepository.existsByNameIgnoreCase(anyString()))
                .thenReturn(false);

        when(collegeDomainRepository.existsByDomain(anyString()))
                .thenReturn(false);

        when(collegeRepository.save(any()))
                .thenReturn(
                        College.builder()
                                .collegeId(UUID.randomUUID())
                                .build()
                );

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(existingAdmin));

        assertThatThrownBy(() ->
                collegeService.setupCollege(setupDto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("college admin");
    }

    @Test
    @DisplayName("existing STUDENT is promoted to COLLEGE_ADMIN")
    void existingUserGetsPromotedToCollegeAdmin() {

        User existing = User.builder()
                .email("admin@gitam.edu")
                .role(UserRole.STUDENT)
                .build();

        when(collegeRepository.existsByNameIgnoreCase(anyString()))
                .thenReturn(false);

        when(collegeDomainRepository.existsByDomain(anyString()))
                .thenReturn(false);

        when(collegeRepository.save(any()))
                .thenReturn(
                        College.builder()
                                .collegeId(UUID.randomUUID())
                                .name("GITAM University")
                                .build()
                );

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(existing));

        when(userRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(userMapper.toResponseDto(any(User.class)))
                .thenReturn(UserResponseDto.builder().build());

        collegeService.setupCollege(setupDto);

        assertThat(existing.getRole())
                .isEqualTo(UserRole.COLLEGE_ADMIN);
    }

    @Test
    @DisplayName("createCollege() succeeds")
    void createCollegeSuccess() {

        CollegeDto dto = CollegeDto.builder()
                .name("Atria")
                .logoUrl("logo.png")
                .build();

        College college = College.builder()
                .collegeId(UUID.randomUUID())
                .name("Atria")
                .build();

        CollegeResponseDto response =
                CollegeResponseDto.builder()
                        .collegeId(college.getCollegeId())
                        .name("Atria")
                        .build();

        when(collegeRepository.existsByNameIgnoreCase("Atria"))
                .thenReturn(false);

        when(collegeMapper.toEntity(dto))
                .thenReturn(college);

        when(collegeRepository.save(college))
                .thenReturn(college);

        when(collegeMapper.toResponseDto(college))
                .thenReturn(response);

        CollegeResponseDto result =
                collegeService.createCollege(dto);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("resolveByDomain() returns active college")
    void resolveByDomainSuccess() {

        College college = College.builder()
                .collegeId(UUID.randomUUID())
                .name("Atria")
                .build();

        CollegeDomain domain = CollegeDomain.builder()
                .domain("atria.edu")
                .college(college)
                .primary(true)
                .build();

        when(collegeDomainRepository
                .findByDomainAndCollege_IsActiveTrueWithCollege("atria.edu"))
                .thenReturn(Optional.of(domain));

        College result =
                collegeService.resolveByDomain("atria.edu");

        assertThat(result).isEqualTo(college);
    }

    @Test
    @DisplayName("setActiveStatus() deactivates college")
    void deactivateCollege() {

        College college = College.builder()
                .collegeId(UUID.randomUUID())
                .name("Atria")
                .isActive(true)
                .build();

        when(collegeRepository.findById(college.getCollegeId()))
                .thenReturn(Optional.of(college));

        when(collegeRepository.save(any()))
                .thenReturn(college);

        when(collegeMapper.toResponseDto(any()))
                .thenReturn(
                        CollegeResponseDto.builder()
                                .collegeId(college.getCollegeId())
                                .isActive(false)
                                .build()
                );

        collegeService.setActiveStatus(
                college.getCollegeId(),
                false);

        assertThat(college.isActive()).isFalse();
    }
}
