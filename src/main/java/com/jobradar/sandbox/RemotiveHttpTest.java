package com.jobradar.sandbox;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobradar.dto.RemotiveJobDto;
import com.jobradar.dto.RemotiveResponseDto;
public class RemotiveHttpTest {

    public static void main(String[] args) throws Exception{

        HttpClient client = HttpClient.newHttpClient();

        URI uri = URI.create(
                "https://remotive.com/api/remote-jobs?limit=1"
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();
        System.out.println(uri);
        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
        System.out.println("状态码：" + response.statusCode());
        String body = response.body();
        ObjectMapper mapper = new ObjectMapper();
        RemotiveResponseDto responseDto =
                mapper.readValue(body, RemotiveResponseDto.class);
        System.out.println("岗位数量：" + responseDto.jobs.size());
        RemotiveJobDto firstJob = responseDto.jobs.get(0);
        System.out.println("公司：" + firstJob.company_name);
        System.out.println("岗位：" + firstJob.title);
        System.out.println("地点：" + firstJob.candidate_required_location);
        int jobsIndex = body.indexOf("\"jobs\"");

        System.out.println("jobs位置：" + jobsIndex);

        if (jobsIndex >= 0) {
            int end = Math.min(jobsIndex + 1800, body.length());
            System.out.println(body.substring(jobsIndex, end));
        }
    }
}