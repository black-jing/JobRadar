package com.jobradar.matching.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jobradar.domain.Job;
import com.jobradar.domain.JobMatchResult;
import com.jobradar.domain.UserProfile;
import com.jobradar.matching.JobMatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
@Component
public class DeepSeekJobMatcher implements JobMatcher {

    private final String apiUrl;
    private final String model;

    public DeepSeekJobMatcher(
            @Value("${deepseek.api-url}") String apiUrl,
            @Value("${deepseek.model}") String model) {

        this.apiUrl = apiUrl;
        this.model = model;
    }

    @Override
    public JobMatchResult match(Job job, UserProfile userProfile) {

        String systemPrompt = """
                你是JobRadar的用户-岗位匹配模块。

                你的任务是根据岗位事实和用户明确提供的画像信息，
                判断用户与岗位的匹配情况。

                匹配和评分时，所有岗位都必须使用同一套判断标准，
                不要因为岗位不同临时改变评分尺度：
                
                1. 目标方向匹配：
                   用户的targetDirection与岗位主要方向是否吻合；
                
                2. 技能匹配：
                   用户的skills与岗位明确要求的技能匹配程度；
                
                3. 经历关联：
                   用户的experienceSummary与岗位职责、岗位要求的关联程度；
                
                4. 主要能力缺口：
                   岗位明确要求、但用户资料中没有体现的关键能力。
                
                score为0到100的业务参考匹配分数，
                综合以上维度判断，并始终使用同一尺度：
                
                80到100：整体匹配较高；
                60到79：有一定匹配，但存在明显缺口；
                0到59：方向、技能或经历关联相对较弱。

                必须遵守：
                - 只能依据提供的岗位事实和用户画像；
                - 不得编造或推测用户没有提供的经历、技能和背景；
                - 未提供的信息视为未知；
                - 不评价薪资；
                - 不预测面试、录用或Offer概率；
                - score只是0到100的业务参考匹配分数，不代表统计概率。

                必须只返回JSON，结构如下：
                {
                  "score": 0,
                  "matchedSkills": ["..."],
                  "gaps": ["..."],
                  "reason": "...",
                  "suggestion": "..."
                }
                """;

        String userPrompt = """
                【岗位JD事实】

                公司：%s
                岗位名称：%s
                工作地点：%s

                岗位JD：
                %s


                【用户画像事实】

                目标方向：
                %s

                已有技能：
                %s

                经历描述：
                %s


                【匹配任务】

                请根据以上事实完成用户与岗位的匹配分析。
                """.formatted(
                job.getCompany(),
                job.getTitle(),
                job.getLocation(),
                job.getDescription(),
                userProfile.getTargetDirection(),
                userProfile.getSkills(),
                userProfile.getExperienceSummary()
        );

        System.out.println("=== Match System Prompt ===");
        System.out.println(systemPrompt);

        System.out.println("=== Match User Prompt ===");
        System.out.println(userPrompt);

        try {

            String apiKey = System.getenv("DEEPSEEK_API_KEY");

            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException(
                        "没有读取到 DEEPSEEK_API_KEY"
                );
            }

            ObjectMapper mapper = new ObjectMapper();

            ObjectNode requestJson = mapper.createObjectNode();

            requestJson.put("model", model);
            requestJson.put("stream", false);
            requestJson.put("max_tokens", 700);

            ArrayNode messages = requestJson.putArray("messages");

            messages.addObject()
                    .put("role", "system")
                    .put("content", systemPrompt);

            messages.addObject()
                    .put("role", "user")
                    .put("content", userPrompt);

            requestJson.putObject("response_format")
                    .put("type", "json_object");

            requestJson.putObject("thinking")
                    .put("type", "disabled");

            String requestBody =
                    mapper.writeValueAsString(requestJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header(
                            "Content-Type",
                            "application/json"
                    )
                    .timeout(Duration.ofSeconds(30))
                    .header(
                            "Authorization",
                            "Bearer " + apiKey
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofString(requestBody)
                    )
                    .build();

            HttpClient client =
                    HttpClient.newHttpClient();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "DeepSeek匹配请求失败，状态码："
                                + response.statusCode()
                                + "，响应："
                                + response.body()
                );
            }

            JsonNode responseJson =
                    mapper.readTree(response.body());

            String content = responseJson
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            if (content == null || content.isBlank()) {
                throw new RuntimeException(
                        "DeepSeek返回的岗位匹配内容为空"
                );
            }

            JsonNode matchJson =
                    mapper.readTree(content);

            if (!matchJson.has("score")
                    || !matchJson.path("score").isIntegralNumber()
                    || !matchJson.path("matchedSkills").isArray()
                    || !matchJson.path("gaps").isArray()
                    || matchJson.path("reason").asText().isBlank()
                    || matchJson.path("suggestion").asText().isBlank()) {

                throw new RuntimeException(
                        "模型返回的岗位匹配结构不符合预期"
                );
            }

            int score = matchJson
                    .path("score")
                    .asInt();
            if (score < 0 || score > 100) {
                throw new RuntimeException(
                        "模型返回的score超出0到100范围：" + score
                );
            }
            String reason = matchJson
                    .path("reason")
                    .asText();

            String suggestion = matchJson
                    .path("suggestion")
                    .asText();

            List<String> matchedSkills =
                    new ArrayList<>();

            for (JsonNode skillNode :
                    matchJson.path("matchedSkills")) {

                matchedSkills.add(
                        skillNode.asText()
                );
            }

            List<String> gaps =
                    new ArrayList<>();

            for (JsonNode gapNode :
                    matchJson.path("gaps")) {

                gaps.add(
                        gapNode.asText()
                );
            }

            return new JobMatchResult(
                    score,
                    matchedSkills,
                    gaps,
                    reason,
                    suggestion
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "DeepSeek岗位匹配失败",
                    e
            );
        }
    }
}