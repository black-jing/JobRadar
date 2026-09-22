package com.jobradar.service;

import com.jobradar.domain.UserProfile;
import com.jobradar.dto.UserProfileRequest;
import com.jobradar.repository.UserProfileRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void saveProfileShouldCreateProfileWhenNoneExists() {

        UserProfileRequest request = createRequest(
                "Java后端",
                List.of("Java", "Spring Boot"),
                "后端开发经历"
        );

        when(userProfileRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = userProfileService.saveProfile(request);

        assertEquals("Java后端", result.getTargetDirection());
        assertEquals(
                List.of("Java", "Spring Boot"),
                result.getSkills()
        );
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void saveProfileShouldUpdateExistingProfile() {

        UserProfile existingProfile = new UserProfile(
                "旧方向",
                List.of("旧技能"),
                "旧经历"
        );
        UserProfileRequest request = createRequest(
                "AI应用开发",
                List.of("Python"),
                "新经历"
        );

        when(userProfileRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(existingProfile));
        when(userProfileRepository.save(existingProfile))
                .thenReturn(existingProfile);

        UserProfile result = userProfileService.saveProfile(request);

        assertSame(existingProfile, result);
        assertEquals("AI应用开发", result.getTargetDirection());
        assertEquals(List.of("Python"), result.getSkills());
        assertEquals("新经历", result.getExperienceSummary());
        verify(userProfileRepository).save(existingProfile);
    }

    @Test
    void saveProfileShouldTrimAndDeduplicateSkills() {

        UserProfileRequest request = createRequest(
                "Java后端",
                List.of(" Java ", "Spring Boot", "Java"),
                "后端开发经历"
        );

        when(userProfileRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = userProfileService.saveProfile(request);

        assertEquals(
                List.of("Java", "Spring Boot"),
                result.getSkills()
        );
    }

    @Test
    void getRequiredProfileShouldFailWhenProfileDoesNotExist() {

        when(userProfileRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> userProfileService.getRequiredProfile()
        );
    }

    private UserProfileRequest createRequest(
            String targetDirection,
            List<String> skills,
            String experienceSummary) {

        UserProfileRequest request = new UserProfileRequest();
        request.setTargetDirection(targetDirection);
        request.setSkills(skills);
        request.setExperienceSummary(experienceSummary);
        return request;
    }
}
