package com.jobradar.repository;

import com.jobradar.domain.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository
        extends JpaRepository<Job, Long> {

    Job findBySourceAndSourceUrl(
            String source,
            String sourceUrl
    );

    @Query("""
            SELECT j
            FROM Job j
            WHERE
                (
                    :keyword IS NULL
                    OR :keyword = ''
                    OR LOWER(j.title)
                        LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(j.company)
                        LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            AND
                (
                    :location IS NULL
                    OR :location = ''
                    OR LOWER(j.location)
                        LIKE LOWER(CONCAT('%', :location, '%'))
                )
            AND
                (
                    :source IS NULL
                    OR :source = ''
                    OR LOWER(j.source)
                        = LOWER(:source)
                )
            ORDER BY j.id DESC
            """)
    Page<Job> searchJobs(
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("source") String source,
            Pageable pageable
    );
}