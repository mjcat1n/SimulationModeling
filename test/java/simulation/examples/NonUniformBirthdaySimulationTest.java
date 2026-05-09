package simulation.examples;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link NonUniformBirthdaySimulation}.
 */
@DisplayName("NonUniformBirthdaySimulation")
class NonUniformBirthdaySimulationTest {

    private static final double[] UNIFORM_MONTH_WEIGHTS = {
        1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
        1.0, 1.0, 1.0, 1.0, 1.0, 1.0
    };

    @Test
    @DisplayName("constructor rejects invalid group size")
    void constructorRejectsInvalidGroupSize() {
        assertThrows(IllegalArgumentException.class,
            () -> new NonUniformBirthdaySimulation(0, 100, 1L));
    }

    @Test
    @DisplayName("constructor rejects invalid trial count")
    void constructorRejectsInvalidTrialCount() {
        assertThrows(IllegalArgumentException.class,
            () -> new NonUniformBirthdaySimulation(23, 0, 1L));
    }

    @Test
    @DisplayName("constructor rejects missing month weights")
    void constructorRejectsMissingMonthWeights() {
        assertThrows(NullPointerException.class,
            () -> new NonUniformBirthdaySimulation(23, 100, 1L, null));
    }

    @Test
    @DisplayName("constructor rejects wrong number of month weights")
    void constructorRejectsWrongNumberOfMonthWeights() {
        assertThrows(IllegalArgumentException.class,
            () -> new NonUniformBirthdaySimulation(23, 100, 1L, new double[] {1.0, 1.0}));
    }

    @Test
    @DisplayName("constructor rejects negative month weight")
    void constructorRejectsNegativeMonthWeight() {
        double[] weights = NonUniformBirthdaySimulation.getDefaultMonthWeights();
        weights[0] = -1.0;

        assertThrows(IllegalArgumentException.class,
            () -> new NonUniformBirthdaySimulation(23, 100, 1L, weights));
    }

    @Test
    @DisplayName("default month weights are copied")
    void defaultMonthWeightsAreCopied() {
        double[] weights = NonUniformBirthdaySimulation.getDefaultMonthWeights();
        weights[0] = 100.0;

        assertNotEquals(weights[0], NonUniformBirthdaySimulation.getDefaultMonthWeights()[0]);
    }

    @Test
    @DisplayName("uniform month weights produce uniform days")
    void uniformMonthWeightsProduceUniformDays() {
        double[] cumulative =
            NonUniformBirthdaySimulation.buildCumulativeDayProbabilities(UNIFORM_MONTH_WEIGHTS);

        assertEquals(1.0 / NonUniformBirthdaySimulation.DAYS_IN_YEAR, cumulative[0], 1e-12);
        assertEquals(2.0 / NonUniformBirthdaySimulation.DAYS_IN_YEAR, cumulative[1], 1e-12);
        assertEquals(1.0, cumulative[NonUniformBirthdaySimulation.DAYS_IN_YEAR - 1], 1e-12);
    }

    @Test
    @DisplayName("same seed and same parameters are repeatable")
    void sameSeedAndSameParametersAreRepeatable() {
        NonUniformBirthdaySimulation first =
            new NonUniformBirthdaySimulation(22, 1_000, 2026L);
        NonUniformBirthdaySimulation second =
            new NonUniformBirthdaySimulation(22, 1_000, 2026L);

        first.run();
        second.run();

        assertEquals(first.getCollisionCount(), second.getCollisionCount());
        assertEquals(first.getEstimatedProbability(), second.getEstimatedProbability(), 1e-12);
    }

    @Test
    @DisplayName("different seeds produce different birthday sequences")
    void differentSeedsProduceDifferentBirthdaySequences() {
        double[] cumulative =
            NonUniformBirthdaySimulation.buildCumulativeDayProbabilities(UNIFORM_MONTH_WEIGHTS);
        Random r1 = new Random(1L);
        Random r2 = new Random(2L);

        boolean diverged = false;
        for (int i = 0; i < 20; i++) {
            if (NonUniformBirthdaySimulation.drawBirthday(r1, cumulative)
                    != NonUniformBirthdaySimulation.drawBirthday(r2, cumulative)) {
                diverged = true;
                break;
            }
        }
        assertTrue(diverged, "seeds 1L and 2L must produce at least one different birthday in 20 draws");
    }

    @Test
    @DisplayName("run completes the requested number of trials")
    void runCompletesRequestedNumberOfTrials() {
        NonUniformBirthdaySimulation simulation =
            new NonUniformBirthdaySimulation(22, 100, 2026L);

        simulation.run();

        assertEquals(22, simulation.getGroupSize());
        assertEquals(100, simulation.getTrialCount());
        assertEquals(2026L, simulation.getSeed());
        assertEquals(100, simulation.getCompletedTrials());
    }

    @Test
    @DisplayName("estimate is collisions divided by completed trials")
    void estimateIsCollisionsDividedByCompletedTrials() {
        NonUniformBirthdaySimulation simulation =
            new NonUniformBirthdaySimulation(22, 100, 2026L);

        simulation.run();

        assertEquals(
            (double) simulation.getCollisionCount() / simulation.getCompletedTrials(),
            simulation.getEstimatedProbability(),
            1e-12);
    }

    @Test
    @DisplayName("estimate is zero before running trials")
    void estimateIsZeroBeforeRunningTrials() {
        NonUniformBirthdaySimulation simulation =
            new NonUniformBirthdaySimulation(22, 100, 2026L);

        assertEquals(0.0, simulation.getEstimatedProbability());
    }

    @Test
    @DisplayName("one-person group cannot have a collision")
    void onePersonGroupCannotHaveCollision() {
        double[] cumulative =
            NonUniformBirthdaySimulation.buildCumulativeDayProbabilities(UNIFORM_MONTH_WEIGHTS);

        assertFalse(NonUniformBirthdaySimulation.hasSharedBirthday(
            1,
            new Random(1L),
            cumulative));
    }

    @Test
    @DisplayName("group larger than possible birthdays must have a collision")
    void groupLargerThanPossibleBirthdaysMustHaveCollision() {
        double[] cumulative =
            NonUniformBirthdaySimulation.buildCumulativeDayProbabilities(UNIFORM_MONTH_WEIGHTS);

        assertTrue(NonUniformBirthdaySimulation.hasSharedBirthday(
            NonUniformBirthdaySimulation.DAYS_IN_YEAR + 1,
            new Random(1L),
            cumulative));
    }

    @Test
    @DisplayName("input month weights are not changed")
    void inputMonthWeightsAreNotChanged() {
        double[] weights = NonUniformBirthdaySimulation.getDefaultMonthWeights();
        double[] original = NonUniformBirthdaySimulation.getDefaultMonthWeights();

        NonUniformBirthdaySimulation.buildCumulativeDayProbabilities(weights);

        assertArrayEquals(original, weights);
    }
}
