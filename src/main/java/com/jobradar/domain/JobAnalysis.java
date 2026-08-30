package com.jobradar.domain;

import java.util.List;

public class JobAnalysis {

    private String direction;
    private List<String> skills;
    private String summary;
    public JobAnalysis() {
    }
    public JobAnalysis(
            String direction,
            List<String> skills,
            String summary) {
        this.direction = direction;
        this.skills = skills;
        this.summary = summary;
    }
    public void setDirection(String direction) {
        this.direction = direction;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public void setSummary(String summary) {
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