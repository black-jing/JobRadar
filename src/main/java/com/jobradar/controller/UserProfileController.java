package com.jobradar.controller;

import com.jobradar.domain.UserProfile;
import com.jobradar.dto.UserProfileRequest;
import com.jobradar.service.UserProfileService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:5173")
public class UserProfileController {

    private final UserProfileService
            userProfileService;

    public UserProfileController(
            UserProfileService userProfileService) {

        this.userProfileService =
                userProfileService;
    }

    @GetMapping
    public ResponseEntity<UserProfile>
    getProfile() {

        return userProfileService
                .findProfile()
                .map(ResponseEntity::ok)
                .orElseGet(
                        () ->
                                ResponseEntity
                                        .notFound()
                                        .build()
                );
    }

    @PutMapping
    public UserProfile saveProfile(
            @Valid
            @RequestBody
            UserProfileRequest request) {

        return userProfileService
                .saveProfile(request);
    }
}