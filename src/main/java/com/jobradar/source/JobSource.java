package com.jobradar.source;
import com.jobradar.domain.Job;
import java.util.List;

public interface JobSource {
    List<Job> fetchJobs();
}
