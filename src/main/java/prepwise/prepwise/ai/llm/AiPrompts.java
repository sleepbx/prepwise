package prepwise.prepwise.ai.llm;

public final class AiPrompts {

    private AiPrompts() {}

    public static final String MENTOR_SYSTEM = """
            You are PrepWise, a FAANG-focused DSA interview analytics engine.
            Your job: translate structured preparation metrics into concise, evidence-backed insights.
            Rules:
            1. Reference specific numbers from the data (coverage%, scores, topics, days, counts).
            2. Every recommendation must state: what to do, why, and expected impact.
            3. Never use generic phrases like "practice more", "stay consistent", or "keep it up".
            4. Sound like a senior engineer reviewing a candidate's analytics dashboard, not a cheerleader.
            5. Max 4 sentences. High information density. No bullet points.
            """;

    public static String studyPlan(String summaryJson) {
        return """
                Explain why these topics were prioritized for this study plan and the expected readiness impact.
                Reference the coverage gaps, company distribution, and days remaining. Be specific about expected gains.
                Data:
                """ + summaryJson;
    }

    public static String weakness(String summaryJson) {
        return """
                Explain why this is the top weakness based on the metrics and its specific impact on interview readiness.
                Reference coverage%, accuracy%, average solve time, and interview importance. Give the single highest-ROI fix.
                Data:
                """ + summaryJson;
    }

    public static String readiness(String summaryJson) {
        return """
                Give a frank, data-backed readiness assessment.
                State the two biggest score drivers, the two biggest drags, and the single highest-ROI action with expected point gain.
                Data:
                """ + summaryJson;
    }

    public static String companyAdvice(String summaryJson) {
        return """
                Compare the user's coverage against this company's interview distribution.
                State the top 2 gaps (current% vs expected%), the estimated readiness gain from closing each, and the recommended focus order.
                Data:
                """ + summaryJson;
    }

    public static String analyticsInsight(String summaryJson) {
        return """
                Interpret these week-over-week preparation metrics.
                Flag any regressions with their magnitude, note positive trends, and give the single most important corrective action with expected impact.
                Data:
                """ + summaryJson;
    }

    public static String dailyCoach(String summaryJson) {
        return """
                Generate a focused daily briefing based on these preparation metrics and the user's prompt.
                Reference the current streak, top weakness with its interview importance, and today's highest-ROI task with an estimated readiness gain.
                Data:
                """ + summaryJson;
    }
}
