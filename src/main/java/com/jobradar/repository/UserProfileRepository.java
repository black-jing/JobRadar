package com.jobradar.repository;

import com.jobradar.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository
        extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile>
    findFirstByOrderByIdAsc();
}