package prepwise.prepwise.ai.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class IntelligenceScoringTest {

    @Test
    void readinessScoreUsesDocumentedWeights() {
        // Coverage 35%, consistency 25%, revision 20%, accuracy 15%, speed 5%
        assertThat(IntelligenceScoring.readinessScore(100, 0, 0, 0, 0)).isCloseTo(35.0, within(1e-9));
        assertThat(IntelligenceScoring.readinessScore(0, 100, 0, 0, 0)).isCloseTo(25.0, within(1e-9));
        assertThat(IntelligenceScoring.readinessScore(0, 0, 100, 0, 0)).isCloseTo(20.0, within(1e-9));
        assertThat(IntelligenceScoring.readinessScore(0, 0, 0, 100, 0)).isCloseTo(15.0, within(1e-9));
        assertThat(IntelligenceScoring.readinessScore(0, 0, 0, 0, 100)).isCloseTo(5.0, within(1e-9));
        assertThat(IntelligenceScoring.readinessScore(100, 100, 100, 100, 100)).isCloseTo(100.0, within(1e-9));
    }

    @Test
    void readinessScoreIsClampedToZeroToHundred() {
        assertThat(IntelligenceScoring.readinessScore(500, 500, 500, 500, 500)).isEqualTo(100.0);
        assertThat(IntelligenceScoring.readinessScore(-50, -50, -50, -50, -50)).isEqualTo(0.0);
    }

    @Test
    void weaknessIndexUsesDocumentedWeights() {
        // Low coverage 30%, solve time 20%, hint usage 20%, poor revision 15%, inconsistency 15%
        assertThat(IntelligenceScoring.weaknessIndex(100, 0, 0, 0, 0)).isCloseTo(30.0, within(1e-9));
        assertThat(IntelligenceScoring.weaknessIndex(0, 100, 0, 0, 0)).isCloseTo(20.0, within(1e-9));
        assertThat(IntelligenceScoring.weaknessIndex(0, 0, 100, 0, 0)).isCloseTo(20.0, within(1e-9));
        assertThat(IntelligenceScoring.weaknessIndex(0, 0, 0, 100, 0)).isCloseTo(15.0, within(1e-9));
        assertThat(IntelligenceScoring.weaknessIndex(0, 0, 0, 0, 100)).isCloseTo(15.0, within(1e-9));
    }

    @Test
    void weaknessClassificationBoundaries() {
        assertThat(IntelligenceScoring.weaknessClassification(30)).isEqualTo("STRONG");
        assertThat(IntelligenceScoring.weaknessClassification(30.1)).isEqualTo("MODERATE");
        assertThat(IntelligenceScoring.weaknessClassification(60)).isEqualTo("MODERATE");
        assertThat(IntelligenceScoring.weaknessClassification(60.1)).isEqualTo("WEAK");
    }

    @Test
    void readinessLevelBoundaries() {
        assertThat(IntelligenceScoring.readinessLevel(34.9)).isEqualTo("EARLY_STAGE");
        assertThat(IntelligenceScoring.readinessLevel(35)).isEqualTo("ON_TRACK");
        assertThat(IntelligenceScoring.readinessLevel(60)).isEqualTo("INTERVIEW_READY");
        assertThat(IntelligenceScoring.readinessLevel(80)).isEqualTo("STRONG_CANDIDATE");
    }

    @Test
    void revisionPriorityWeightsAllFourFactorsEqually() {
        assertThat(IntelligenceScoring.revisionPriority(100, 0, 0, 0)).isCloseTo(25.0, within(1e-9));
        assertThat(IntelligenceScoring.revisionPriority(0, 100, 0, 0)).isCloseTo(25.0, within(1e-9));
        assertThat(IntelligenceScoring.revisionPriority(0, 0, 100, 0)).isCloseTo(25.0, within(1e-9));
        assertThat(IntelligenceScoring.revisionPriority(0, 0, 0, 100)).isCloseTo(25.0, within(1e-9));
    }

    @Test
    void urgencyCategoryUsesScoreOrAge() {
        assertThat(IntelligenceScoring.urgencyCategory(70, 0)).isEqualTo("IMMEDIATE");
        assertThat(IntelligenceScoring.urgencyCategory(10, 22)).isEqualTo("IMMEDIATE");
        assertThat(IntelligenceScoring.urgencyCategory(40, 0)).isEqualTo("THIS_WEEK");
        assertThat(IntelligenceScoring.urgencyCategory(10, 11)).isEqualTo("THIS_WEEK");
        assertThat(IntelligenceScoring.urgencyCategory(39.9, 10)).isEqualTo("LATER");
    }
}
