package prepwise.prepwise.ai.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LLMServiceTest {

    private final GeminiClient gemini = mock(GeminiClient.class);
    private final GroqClient groq = mock(GroqClient.class);

    private LLMService service(int requestBudgetPerMinute) {
        return new LLMService(gemini, groq, new ObjectMapper(), "groq", true, 4000, requestBudgetPerMinute, 160);
    }

    private Map<String, Object> readiness(double score) {
        return Map.of("score", score, "level", "ON_TRACK", "expectedOutcome", "Phone Screen Ready", "hiringRisk", "MEDIUM");
    }

    @Test
    void fallsBackToRuleBasedTextWhenNoProviderIsEnabled() {
        when(groq.isEnabled()).thenReturn(false);
        when(gemini.isEnabled()).thenReturn(false);

        String answer = service(4).generateReadinessExplanation(readiness(55.0));

        assertThat(answer).startsWith("Readiness score: 55.0");
    }

    @Test
    void failsOverToGeminiWhenGroqReturnsNothing() {
        when(groq.isEnabled()).thenReturn(true);
        when(groq.generate(anyString(), anyString())).thenReturn(null);
        when(gemini.isEnabled()).thenReturn(true);
        when(gemini.generate(anyString(), anyString())).thenReturn("gemini answer");

        String answer = service(4).generateReadinessExplanation(readiness(60.0));

        assertThat(answer).isEqualTo("gemini answer");
        verify(groq).generate(anyString(), anyString());
        verify(gemini).generate(anyString(), anyString());
    }

    @Test
    void cachesAnswersForTheSamePrompt() {
        when(groq.isEnabled()).thenReturn(true);
        when(groq.generate(anyString(), anyString())).thenReturn("groq answer");
        LLMService llm = service(4);

        llm.generateReadinessExplanation(readiness(70.0));
        String second = llm.generateReadinessExplanation(readiness(70.0));

        assertThat(second).isEqualTo("groq answer");
        verify(groq, times(1)).generate(anyString(), anyString());
    }

    @Test
    void requestBudgetStopsProviderCallsAndReturnsFallback() {
        when(groq.isEnabled()).thenReturn(true);
        when(groq.generate(anyString(), anyString())).thenReturn("groq answer");
        LLMService llm = service(1);

        String first = llm.generateReadinessExplanation(readiness(40.0));
        String second = llm.generateReadinessExplanation(readiness(41.0));

        assertThat(first).isEqualTo("groq answer");
        assertThat(second).startsWith("Readiness score: 41.0");
        verify(groq, times(1)).generate(anyString(), anyString());
    }
}
