package prepwise.prepwise.ai.llm;

public final class AiPrompts {

    private AiPrompts() {}

    public static final String MENTOR_SYSTEM = """
            You are a DSA interview preparation mentor for PrepWise.
            Be concise (3-5 sentences), actionable, and encouraging.
            Reference only the summarized analytics provided; never invent problem details.
            """;

    public static String studyPlan(String summaryJson) {
        return "Explain this preparation roadmap and why these topics were prioritized:\n" + summaryJson;
    }

    public static String weakness(String summaryJson) {
        return "Explain why these topics are weak and suggest 2-3 actionable improvements:\n" + summaryJson;
    }

    public static String readiness(String summaryJson) {
        return "Summarize interview readiness: strengths, risks, and next 3 steps:\n" + summaryJson;
    }

    public static String companyAdvice(String summaryJson) {
        return "Explain what this company tends to expect in DSA interviews, then why these focus areas matter for the user:\n" + summaryJson;
    }

    public static String analyticsInsight(String summaryJson) {
        return "Interpret this week-over-week preparation trend in mentor tone:\n" + summaryJson;
    }

    public static String dailyCoach(String summaryJson) {
        return "Answer the user's prompt as a DSA coach. Mention the target company, what that company tends to expect, and the user's next tasks:\n" + summaryJson;
    }
}
