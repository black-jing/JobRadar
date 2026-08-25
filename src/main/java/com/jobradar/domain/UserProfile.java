package com.jobradar.domain;

import java.util.List;

public class UserProfile {

    private String targetDirection;
    private List<String> skills;
    private String experienceSummary;
    public UserProfile() {
    }
    public UserProfile(
            String targetDirection,
            List<String> skills,
            String experienceSummary) {

        this.targetDirection = targetDirection;
        this.skills = skills;
        this.experienceSummary = experienceSummary;
    }

    public String getTargetDirection() {
        return targetDirection;
    }

    public List<String> getSkills() {
        return skills;
    }

    public String getExperienceSummary() {
        return experienceSummary;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "targetDirection='" + targetDirection + '\'' +
                ", skills=" + skills +
                ", experienceSummary='" + experienceSummary + '\'' +
                '}';
    }
}