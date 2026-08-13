package com.jobradar.sandbox;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class JacksonSmokeTest {

    public static void main(String[] args) throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        Map<String, String> job = Map.of(
                "company", "字节跳动",
                "title", "Java后端实习生",
                "location", "北京"
        );

        String json = mapper.writeValueAsString(job);

        System.out.println(json);
    }
}