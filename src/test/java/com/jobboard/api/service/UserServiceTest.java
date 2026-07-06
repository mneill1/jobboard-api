package com.jobboard.api.service;

import com.jobboard.api.dto.ApplicationResponse;
import com.jobboard.api.dto.JobResponse;
import com.jobboard.api.dto.UpdateProfileRequest;
import com.jobboard.api.dto.UserProfileResponse;
import com.jobboard.api.entity.*;
import com.jobboard.api.repository.ApplicationRepository;
import com.jobboard.api.repository.CompanyRepository;
import com.jobboard.api.repository.JobRepository;
import com.jobboard.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private JobRepository jobRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    private User applicantUser;
    private User companyUser;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(10L);
        company.setName("Acme");
        company.setIndustry("Tech");

        applicantUser = new User();
        applicantUser.setId(1L);
        applicantUser.setEmail("alice@test.com");
        applicantUser.setPassword("hashed");
        applicantUser.setRole(UserRole.APPLICANT);

        companyUser = new User();
        companyUser.setId(2L);
        companyUser.setEmail("co@test.com");
        companyUser.setPassword("hashed");
        companyUser.setRole(UserRole.COMPANY);
        companyUser.setCompanyId(10L);
    }

    // --- getProfile ---

    @Test
    void getProfile_applicant_returnsProfileWithoutCompany() {
        UserProfileResponse profile = userService.getProfile(applicantUser);

        assertThat(profile.getEmail()).isEqualTo("alice@test.com");
        assertThat(profile.getRole()).isEqualTo("APPLICANT");
        assertThat(profile.getCompany()).isNull();
        verifyNoInteractions(companyRepository);
    }

    @Test
    void getProfile_companyUser_includesLinkedCompany() {
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));

        UserProfileResponse profile = userService.getProfile(companyUser);

        assertThat(profile.getRole()).isEqualTo("COMPANY");
        assertThat(profile.getCompanyId()).isEqualTo(10L);
        assertThat(profile.getCompany()).isNotNull();
        assertThat(profile.getCompany().getName()).isEqualTo("Acme");
    }

    // --- getApplications ---

    @Test
    void getApplications_returnsApplicationsForUser() {
        Application app = new Application();
        app.setId("app-1");
        app.setJobId(5L);
        app.setUserId(1L);
        app.setApplicantName("Alice");
        app.setEmail("alice@test.com");
        app.setStatus("RECEIVED");

        when(applicationRepository.findByUserId(1L)).thenReturn(List.of(app));

        List<ApplicationResponse> results = userService.getApplications(applicantUser);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getApplicantName()).isEqualTo("Alice");
    }

    @Test
    void getApplications_noApplications_returnsEmptyList() {
        when(applicationRepository.findByUserId(1L)).thenReturn(List.of());

        assertThat(userService.getApplications(applicantUser)).isEmpty();
    }

    // --- getJobs ---

    @Test
    void getJobs_companyUser_returnsCompanyJobs() {
        Job job = new Job();
        job.setId(20L);
        job.setTitle("Engineer");
        job.setStatus(JobStatus.ACTIVE);
        job.setCompany(company);

        when(jobRepository.findByCompany_Id(10L)).thenReturn(List.of(job));

        List<JobResponse> results = userService.getJobs(companyUser);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Engineer");
        assertThat(results.get(0).getCompanyName()).isEqualTo("Acme");
    }

    // --- updateProfile ---

    @Test
    void updateProfile_newEmail_updatesEmailAndSaves() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setEmail("new@test.com");

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(applicantUser)).thenReturn(applicantUser);

        userService.updateProfile(applicantUser, req);

        assertThat(applicantUser.getEmail()).isEqualTo("new@test.com");
        verify(userRepository).save(applicantUser);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateProfile_newPassword_encodesAndSaves() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setPassword("newpassword");

        when(passwordEncoder.encode("newpassword")).thenReturn("new-hashed");
        when(userRepository.save(applicantUser)).thenReturn(applicantUser);

        userService.updateProfile(applicantUser, req);

        assertThat(applicantUser.getPassword()).isEqualTo("new-hashed");
        verify(userRepository).save(applicantUser);
    }

    @Test
    void updateProfile_emailAlreadyTakenByAnotherUser_throwsRuntimeException() {
        User otherUser = new User();
        otherUser.setId(99L);
        otherUser.setEmail("taken@test.com");

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setEmail("taken@test.com");

        when(userRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> userService.updateProfile(applicantUser, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already in use");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_sameEmailAsSelf_doesNotThrow() {
        // The user is updating to their own current email — should be allowed
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setEmail("alice@test.com");

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(applicantUser));
        when(userRepository.save(applicantUser)).thenReturn(applicantUser);

        assertThatNoException().isThrownBy(() -> userService.updateProfile(applicantUser, req));
    }
}
