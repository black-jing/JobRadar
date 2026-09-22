package com.jobradar.service;

import com.jobradar.domain.UserProfile;
import com.jobradar.dto.UserProfileRequest;
import com.jobradar.repository.UserProfileRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserProfileService {

    private final UserProfileRepository
            userProfileRepository;

    public UserProfileService(
            UserProfileRepository userProfileRepository) {

        this.userProfileRepository =
                userProfileRepository;
    }

    @Transactional
    public UserProfile saveProfile(
            UserProfileRequest request) {

        List<String> skills =
                request.getSkills()
                        .stream()
                        .map(String::trim)
                        .filter(skill ->
                                !skill.isBlank())
                        .distinct()
                        .toList();

        if (skills.isEmpty()) {
            throw new IllegalArgumentException(
                    "技能列表不能为空"
            );
        }

        Optional<UserProfile> existingProfile =
                userProfileRepository
                        .findFirstByOrderByIdAsc();

        if (existingProfile.isPresent()) {

            UserProfile profile =
                    existingProfile.get();

            profile.update(
                    request
                            .getTargetDirection()
                            .trim(),
                    skills,
                    request
                            .getExperienceSummary()
                            .trim()
            );

            return userProfileRepository
                    .save(profile);
        }

        UserProfile profile =
                new UserProfile(
                        request
                                .getTargetDirection()
                                .trim(),
                        skills,
                        request
                                .getExperienceSummary()
                                .trim()
                );

        return userProfileRepository
                .save(profile);
    }

    public Optional<UserProfile> findProfile() {

        return userProfileRepository
                .findFirstByOrderByIdAsc();
    }

    public UserProfile getRequiredProfile() {

        return findProfile()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "请先保存用户画像"
                                )
                );
    }
}