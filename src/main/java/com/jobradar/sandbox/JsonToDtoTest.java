package com.jobradar.sandbox;
import com.jobradar.domain.Job;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobradar.dto.ExternalJobDto;

public class JsonToDtoTest {

    public static void main(String[] args) throws Exception {

        String json = """
                {
                  "company_name": "ByteDance",
                  "title": "Java Backend Intern",
                  "candidate_required_location": "Beijing",
                  "url": "https://example.com/jobs/123"
                }
                """;

        ObjectMapper mapper = new ObjectMapper();

        ExternalJobDto dto =
                mapper.readValue(json, ExternalJobDto.class);
        Job job = new Job(
                dto.company_name,
                dto.title,
                dto.candidate_required_location,
                null,
                null,
                "LocalJson",
                dto.url
        );
        System.out.println(dto.company_name);
        System.out.println(dto.title);
        System.out.println(dto.candidate_required_location);
        System.out.println(dto.url);
        System.out.println(job);
    }
}