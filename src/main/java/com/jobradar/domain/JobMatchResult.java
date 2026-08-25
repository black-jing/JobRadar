package com.jobradar.domain;

import java.util.List;

public class JobMatchResult {

    private int score;
    private List<String> matchedSkills;
    private List<String> gaps;
    private String reason;
    private String suggestion;

    public JobMatchResult(
            int score,
            List<String> matchedSkills,
            List<String> gaps,
            String reason,
            String suggestion) {

        this.score = score;
        this.matchedSkills = matchedSkills;
        this.gaps = gaps;
        this.reason = reason;
        this.suggestion = suggestion;
    }

    public int getScore() {
        return score;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public List<String> getGaps() {
        return gaps;
    }

    public String getReason() {
        return reason;
    }

    public String getSuggestion() {
        return suggestion;
    }

    @Override
    public String toString() {
        return "JobMatchResult{" +
                "score=" + score +
                ", matchedSkills=" + matchedSkills +
                ", gaps=" + gaps +
                ", reason='" + reason + '\'' +
                ", suggestion='" + suggestion + '\'' +
                '}';
    }
}