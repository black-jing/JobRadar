package com.jobradar.domain;

import java.util.List;

public class JobAnalysis {

    private String direction;
    private List<String> skills;
    private String summary;
    public JobAnalysis(
            String direction,
            List<String> skills,
            String summary) {
        this.direction = direction;
        this.skills = skills;
        this.summary = summary;
    }
    public String getDirection() {
        return direction;
    }

    public List<String> getSkills() {
        return skills;
    }

    public String getSummary() {
        return summary;
    }
    @Override
    public String toString() {
        return "JobAnalysis{" +
                "direction='" + direction + '\'' +
                ", skills=" + skills +
                ", summary='" + summary + '\'' +
                '}';
    }
}