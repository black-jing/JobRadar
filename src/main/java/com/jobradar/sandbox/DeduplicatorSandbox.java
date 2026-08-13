package com.jobradar.sandbox;

import com.jobradar.deduplication.JobDeduplicator;
import com.jobradar.domain.Job;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DeduplicatorSandbox {

    public static void main(String[] args) {
Job job1=new Job("腾讯","java工程师",
        "shenzhen",
        "haogongz",
        LocalDate.of(2026,6,7),
        "String ,ource",
        "Strig sourceUrl");
        Job job2=new Job("腾讯","java工程师",
                "shenzhen",
                "haogongz",
                LocalDate.of(2026,6,7),
                "String ,ource",
                "Strig sourceUrl");

    }
}