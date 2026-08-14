package com.jobradar.sandbox;
import com.jobradar.domain.JobAnalysis;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
public class DeepSeekSmokeTest {

    public static void main(String[] args)  throws Exception {

        String apiKey = System.getenv("DEEPSEEK_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "没有读取到 DEEPSEEK_API_KEY"
            );
        }
        String requestBody = """
        {
          "model": "deepseek-v4-flash",
          "messages": [
            {
              "role": "system",
              "content": "你是JobRadar的岗位分析模块。请根据岗位信息提取岗位方向direction、核心技能skills和简短摘要summary。direction使用简短岗位方向名称；skills只包含核心技术、工具和专业技能；summary用1到2句话概括主要工作和核心要求。只返回JSON。"
            },
            {
              "role": "user",
              "content": "公司：Example Cloud\\n岗位名称：Backend Software Engineer\\n工作地点：Remote\\n岗位描述：We are looking for a backend engineer. Candidates should have experience with Java, Spring Boot, REST APIs and SQL. Docker experience is preferred."
            }
          ],
          "response_format": {
            "type": "json_object"
          },
          "thinking": {
            "type": "disabled"
          },
          "stream": false
        }
        """;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://api.deepseek.com/chat/completions"
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
        System.out.println(
                "状态码：" + response.statusCode()
        );

        System.out.println(
                "原始响应："
        );

        System.out.println(response.body());
        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readTree(response.body());

        String content = root
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();

        System.out.println("模型真正返回的内容：");
        System.out.println(content);
        JsonNode analysisJson = mapper.readTree(content);

        String direction = analysisJson
                .path("direction")
                .asText();

        String summary = analysisJson
                .path("summary")
                .asText();
        List<String> skills = new ArrayList<>();

        for (JsonNode skillNode : analysisJson.path("skills")) {
            skills.add(skillNode.asText());
        }
        JobAnalysis analysis = new JobAnalysis(
                direction,
                skills,
                summary
        );
        System.out.println("转换后的JobAnalysis：");
        System.out.println(analysis);

    }
}