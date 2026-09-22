package com.jobradar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class UserProfileRequest {

    @NotBlank
    private String targetDirection;

    @NotEmpty
    private List<@NotBlank String> skills;

    @NotBlank
    private String experienceSummary;

    public String getTargetDirection() {
        return targetDirection;
    }

    public void setTargetDirection(
            String targetDirection) {

        this.targetDirection =
                targetDirection;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(
            List<String> skills) {

        this.skills = skills;
    }

    public String getExperienceSummary() {
        return experienceSummary;
    }

    public void setExperienceSummary(
            String experienceSummary) {

        this.experienceSummary =
                experienceSummary;
    }
}