package com.jobradar.selection;

import com.jobradar.domain.Job;
import com.jobradar.domain.UserProfile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JobCandidateSelector {

    public List<Job> selectCandidates(
            List<Job> jobs,
            UserProfile userProfile,
            int maxCandidates) {

        List<Job> candidates = new ArrayList<>();

        if (jobs == null
                || jobs.isEmpty()
                || userProfile == null
                || maxCandidates <= 0) {

            return candidates;
        }

        for (Job job : jobs) {

            if (job == null) {
                continue;
            }

            int candidateScore =
                    calculateCandidateScore(
                            job,
                            userProfile
                    );

            if (candidateScore > 0) {
                candidates.add(job);
            }
        }

        candidates.sort(
                Comparator.comparingInt(
                        (Job job) ->
                                calculateCandidateScore(
                                        job,
                                        userProfile
                                )
                ).reversed()
        );

        int limit =
                Math.min(
                        maxCandidates,
                        candidates.size()
                );

        return new ArrayList<>(
                candidates.subList(
                        0,
                        limit
                )
        );
    }

    private int calculateCandidateScore(
            Job job,
            UserProfile userProfile) {

        int score = 0;

        String title =
                job.getTitle() == null
                        ? ""
                        : job.getTitle().toLowerCase();

        String description =
                job.getDescription() == null
                        ? ""
                        : job.getDescription().toLowerCase();

        String targetDirection =
                userProfile.getTargetDirection();

        if (targetDirection != null
                && !targetDirection.isBlank()) {

            String direction =
                    targetDirection.toLowerCase();

            if (title.contains(direction)) {
                score += 3;
            }

            if (description.contains(direction)) {
                score += 1;
            }
        }

        List<String> skills =
                userProfile.getSkills();

        if (skills != null) {

            for (String skill : skills) {

                if (skill == null
                        || skill.isBlank()) {
                    continue;
                }

                String normalizedSkill =
                        skill.toLowerCase();

                if (title.contains(normalizedSkill)) {
                    score += 2;
                }

                if (description.contains(normalizedSkill)) {
                    score += 1;
                }
            }
        }

        return score;
    }
}