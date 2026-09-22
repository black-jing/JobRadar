package com.jobradar.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobradar.domain.Job;
import com.jobradar.dto.XiaozhaoRadarJobDto;
import com.jobradar.dto.XiaozhaoRadarResponseDto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class XiaozhaoRadarJobSource implements JobSource {

    private static final String API_URL =
            "https://jiabaobei.github.io/xiaozhao-radar/jobs.json";

    @Override
    public List<Job> fetchJobs() {

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println(
                    "XiaozhaoRadar状态码：" + response.statusCode()
            );

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "XiaozhaoRadar请求失败，HTTP "
                                + response.statusCode()
                );
            }

            ObjectMapper mapper = new ObjectMapper();

            XiaozhaoRadarResponseDto responseDto =
                    mapper.readValue(
                            response.body(),
                            XiaozhaoRadarResponseDto.class
                    );

            List<Job> jobs = new ArrayList<>();

            for (XiaozhaoRadarJobDto dto : responseDto.jobs) {

                // 当前数据库用 source + sourceUrl 防重复。
                // 没有真实投递链接的岗位第一版先跳过。
                if (dto.u == null || dto.u.isBlank()) {
                    continue;
                }

                if (dto.c == null || dto.c.isBlank()) {
                    continue;
                }

                if (dto.p == null || dto.p.isBlank()) {
                    continue;
                }

                String description =
                        "岗位/方向：" + safe(dto.p)
                                + "\n招聘批次：" + safe(dto.w)
                                + "\n截止时间：" + safe(dto.d)
                                + "\n行业：" + safe(dto.ind);
                if (!isRelevantInternship(dto)) {
                    continue;
                }
                Job job = new Job(
                        dto.c,
                        dto.p,
                        dto.l,
                        description,
                        null,
                        "XiaozhaoRadar",
                        dto.u
                );

                jobs.add(job);
            }

            System.out.println(
                    "XiaozhaoRadar数据更新时间："
                            + responseDto.updated
            );

            System.out.println(
                    "XiaozhaoRadar最终岗位数量："
                            + jobs.size()
            );

            return jobs;

        } catch (Exception e) {

            System.out.println(
                    "XiaozhaoRadar获取失败："
                            + e.getMessage()
            );

            return List.of();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isRelevantInternship(XiaozhaoRadarJobDto dto) {

        String position = safe(dto.p).toLowerCase();
        String batch = safe(dto.w).toLowerCase();

        boolean internship =
                batch.contains("实习");

        boolean relevant =
                position.contains("java")
                        || position.contains("后端")
                        || position.contains("服务端")
                        || position.contains("开发")
                        || position.contains("ai")
                        || position.contains("大模型")
                        || position.contains("人工智能");

        return internship && relevant;
    }
}