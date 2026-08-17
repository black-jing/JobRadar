package com.jobradar.analysis.deepseek;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jobradar.analysis.JobAnalyzer;
import com.jobradar.domain.Job;
import com.jobradar.domain.JobAnalysis;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
@Component
public class DeepSeekJobAnalyzer implements JobAnalyzer {
    private final String apiUrl;
    private final String model;
    public DeepSeekJobAnalyzer(
            @Value("${deepseek.api-url}") String apiUrl,
            @Value("${deepseek.model}") String model) {

        this.apiUrl = apiUrl;
        this.model = model;
    }
    @Override
    public JobAnalysis analyze(Job job) throws JsonProcessingException {
        String systemPrompt = """
        你是JobRadar的岗位分析模块。

        请根据岗位信息提取：
        1. direction：简短的岗位方向名称；
        2. skills：核心技术、工具和专业技能，不包含泛泛的软技能；
        3. summary：用1到2句话概括主要工作内容和核心要求，不提供求职建议。

        必须只返回JSON，结构如下：
        {
          "direction": "...",
          "skills": ["...", "..."],
          "summary": "..."
        }
        """;
        String userPrompt = """
        公司：%s
        岗位名称：%s
        工作地点：%s

        岗位描述：
        %s
        """.formatted(
                job.getCompany(),
                job.getTitle(),
                job.getLocation(),
                job.getDescription()
        );
        System.out.println("=== System Prompt ===");
        System.out.println(systemPrompt);

        System.out.println("=== User Prompt ===");
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
            requestJson.put("max_tokens", 500);
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
                    .uri(URI.create(
                            apiUrl
                    ))
                    .header("Content-Type", "application/json")
                    .header(
                            "Authorization",
                            "Bearer " + apiKey
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofString(requestBody)
                    )
                    .build();
            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "DeepSeek请求失败，状态码："
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
                        "DeepSeek返回的岗位分析内容为空"
                );
            }
            JsonNode analysisJson =
                    mapper.readTree(content);
            if (analysisJson.path("direction").asText().isBlank()
                    || !analysisJson.path("skills").isArray()
                    || analysisJson.path("summary").asText().isBlank()) {

                throw new RuntimeException(
                        "模型返回结构不符合预期"
                );
            }
            String direction = analysisJson
                    .path("direction")
                    .asText();

            String summary = analysisJson
                    .path("summary")
                    .asText();
            List<String> skills = new ArrayList<>();

            for (JsonNode skillNode :
                    analysisJson.path("skills")) {

                skills.add(skillNode.asText());
            }
            return new JobAnalysis(
                    direction,
                    skills,
                    summary
            );
        }catch(Exception e){
            throw new RuntimeException(
                    "DeepSeek岗位分析失败",
                    e
            );
        }
    }
}