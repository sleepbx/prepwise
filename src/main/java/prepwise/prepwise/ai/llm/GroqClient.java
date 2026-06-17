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
public class GroqClient {

    private static final String CHAT_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final boolean enabled;
    private final int maxOutputTokens;

    public GroqClient(
            ObjectMapper objectMapper,
            @Value("${prepwise.ai.groq.api-key:${GROQ_API_KEY:}}") String apiKey,
            @Value("${prepwise.ai.groq.model:${GROQ_MODEL:llama-3.1-8b-instant}}") String model,
            @Value("${prepwise.ai.max-output-tokens:160}") int maxOutputTokens,
            @Value("${prepwise.ai.enabled:false}") boolean aiEnabled) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.maxOutputTokens = Math.max(80, maxOutputTokens);
        this.enabled = aiEnabled && StringUtils.hasText(apiKey);
        this.restClient = RestClient.builder().build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String generate(String systemPrompt, String userPrompt) {
        if (!enabled) return null;
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", 0.4,
                    "max_tokens", maxOutputTokens,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );

            String response = restClient.post()
                    .uri(CHAT_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0)
                    .path("message").path("content").asText();
        } catch (Exception e) {
            log.warn("Groq API call failed: {}", e.getMessage());
            return null;
        }
    }
}
