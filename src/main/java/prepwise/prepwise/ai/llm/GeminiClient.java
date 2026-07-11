package prepwise.prepwise.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public GeminiClient(
            ObjectMapper objectMapper,
            @Value("${spring.ai.google.genai.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${spring.ai.google.genai.chat.options.model:gemini-2.0-flash-001}") String model,
            @Value("${prepwise.ai.enabled:false}") boolean aiEnabled) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = aiEnabled && StringUtils.hasText(apiKey);
        this.restClient = RestClient.builder().build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String generate(String systemPrompt, String userPrompt) {
        if (!enabled) return null;
        try {
            String url = BASE_URL + model + ":generateContent?key=" + apiKey;
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of(
                                    "text", systemPrompt + "\n\n" + userPrompt
                            ))
                    )),
                    "generationConfig", Map.of(
                            "temperature", 0.4,
                            "maxOutputTokens", 220
                    )
            );

            String response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } catch (Exception e) {
            log.warn("Gemini API call failed: {}", e.getMessage());
            return null;
        }
    }
}
