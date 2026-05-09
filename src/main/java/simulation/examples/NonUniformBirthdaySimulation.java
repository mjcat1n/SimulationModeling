package simulation.examples;

import java.util.Arrays;
import java.util.Random;
import simulation.Event;
import simulation.Simulation;

/**
 * Estimates birthday-collision probability when birthdays are not equally likely.
 *
 * <p>This model keeps the usual birthday-problem assumptions that birthdays are
 * independent and leap years are ignored. The change is that each month has a
 * relative birth weight, so days in high-weight months are more likely than days
 * in low-weight months.
 *
 * <p>Run it after compiling with:
 * <pre>{@code
 * java -cp build/classes simulation.examples.NonUniformBirthdaySimulation 22 10000 1
 * }</pre>
 */
public final class NonUniformBirthdaySimulation extends Simulation {

    /** Number of possible birthdays when leap years are ignored. */
    public static final int DAYS_IN_YEAR = 365;

    /** Number of months in the modeled year. */
    public static final int MONTHS_IN_YEAR = 12;

    private static final int DEFAULT_GROUP_SIZE = 22;
    private static final int DEFAULT_TRIAL_COUNT = 10_000;
    private static final long DEFAULT_SEED = 1L;

    private static final int[] MONTH_LENGTHS = {
        31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    };

    /*
     * Relative daily birth weights by month, January through December.
     * September and August are intentionally higher than winter months so the
     * assignment can compare a clear non-uniform model with the uniform model.
     */
    private static final double[] DEFAULT_MONTH_WEIGHTS = {
        0.70, 0.72, 0.78, 0.86, 0.96, 1.06,
        1.18, 1.35, 1.42, 1.18, 0.98, 0.80
    };

    private final int groupSize;
    private final int trialCount;
    private final long seed;
    private final Random random;
    private final double[] cumulativeDayProbabilities;

    private int completedTrials;
    private int collisionCount;

    /**
     * Creates a simulation using the default non-uniform monthly weights.
     *
     * @param groupSize number of people in each trial group
     * @param trialCount number of independent trials to run
     * @param seed deterministic seed for the random birthday generator
     */
    public NonUniformBirthdaySimulation(int groupSize, int trialCount, long seed) {
        this(groupSize, trialCount, seed, DEFAULT_MONTH_WEIGHTS);
    }

    /**
     * Creates a simulation using caller-provided monthly birth weights.
     *
     * @param groupSize number of people in each trial group
     * @param trialCount number of independent trials to run
     * @param seed deterministic seed for the random birthday generator
     * @param monthWeights relative daily birth weights for January through December
     * @throws IllegalArgumentException if a parameter is outside the model range
     */
    public NonUniformBirthdaySimulation(
            int groupSize,
            int trialCount,
            long seed,
            double... monthWeights) {
        if (groupSize < 1) {
            throw new IllegalArgumentException("Group size must be at least one: " + groupSize);
        }
        if (trialCount < 1) {
            throw new IllegalArgumentException("Trial count must be at least one: " + trialCount);
        }

        this.groupSize = groupSize;
        this.trialCount = trialCount;
        this.seed = seed;
        this.random = new Random(seed);
        this.cumulativeDayProbabilities = buildCumulativeDayProbabilities(monthWeights);
    }

    /**
     * Runs the simulation from the command line.
     *
     * <p>Arguments are optional and interpreted as group size, trial count, and
     * seed. Defaults are group size 22, 10,000 trials, and seed 1.
     *
     * @param args optional {@code groupSize trialCount seed}
     */
    public static void main(String[] args) { 
        int groupSize = parseIntArg(args, 0, DEFAULT_GROUP_SIZE, "groupSize");
        int trialCount = parseIntArg(args, 1, DEFAULT_TRIAL_COUNT, "trialCount");
        long seed = parseLongArg(args, 2, DEFAULT_SEED, "seed");
        if (args.length > 3) {
            throw new IllegalArgumentException(
                "Usage: NonUniformBirthdaySimulation [groupSize] [trialCount] [seed]");
        }

        NonUniformBirthdaySimulation simulation =
            new NonUniformBirthdaySimulation(groupSize, trialCount, seed);
        long start = System.nanoTime();
        simulation.run();
        long runtimeMillis = (System.nanoTime() - start) / 1_000_000L;

        System.out.printf(
            "model=non-uniform, k=%d, trials=%d, seed=%d, collisions=%d, estimate=%.5f, runtime=%d ms%n",
            simulation.getGroupSize(),
            simulation.getTrialCount(),
            simulation.getSeed(),
            simulation.getCollisionCount(),
            simulation.getEstimatedProbability(),
            runtimeMillis);
    }

    /**
     * Returns a copy of the default monthly relative weights.
     *
     * @return relative daily weights for January through December
     */
    public static double[] getDefaultMonthWeights() {
        return Arrays.copyOf(DEFAULT_MONTH_WEIGHTS, DEFAULT_MONTH_WEIGHTS.length);
    }

    /**
     * Builds a cumulative birthday distribution from monthly relative weights.
     *
     * <p>Each day inside a month receives the same relative daily weight. The
     * resulting cumulative array has 365 entries and ends at exactly 1.0.
     *
     * @param monthWeights relative daily birth weights for January through December
     * @return cumulative probability array for days 0 through 364
     * @throws IllegalArgumentException if the weights are invalid
     */
    public static double[] buildCumulativeDayProbabilities(double... monthWeights) {
        validateMonthWeights(monthWeights);

        double totalWeight = 0.0;
        for (int month = 0; month < MONTHS_IN_YEAR; month++) {
            totalWeight += monthWeights[month] * MONTH_LENGTHS[month];
        }

        double[] cumulative = new double[DAYS_IN_YEAR];
        double runningTotal = 0.0;
        int dayOfYear = 0;
        for (int month = 0; month < MONTHS_IN_YEAR; month++) {
            double dayProbability = monthWeights[month] / totalWeight;
            for (int dayOfMonth = 0; dayOfMonth < MONTH_LENGTHS[month]; dayOfMonth++) {
                runningTotal += dayProbability;
                cumulative[dayOfYear] = runningTotal;
                dayOfYear++;
            }
        }
        cumulative[DAYS_IN_YEAR - 1] = 1.0;
        return cumulative;
    }

    /**
     * Returns whether one generated group contains a shared birthday.
     *
     * @param groupSize number of birthdays to generate
     * @param random source of random birthdays
     * @param cumulativeDayProbabilities cumulative distribution over 365 birthdays
     * @return {@code true} when any birthday appears more than once
     * @throws IllegalArgumentException if {@code groupSize} is less than one
     * @throws NullPointerException if {@code random} or the distribution is {@code null}
     */
    public static boolean hasSharedBirthday(
            int groupSize,
            Random random,
            double... cumulativeDayProbabilities) {
        if (groupSize < 1) {
            throw new IllegalArgumentException("Group size must be at least one: " + groupSize);
        }
        if (random == null) {
            throw new NullPointerException("Random source must not be null");
        }
        validateCumulativeDayProbabilities(cumulativeDayProbabilities);

        boolean[] observedBirthdays = new boolean[DAYS_IN_YEAR];
        for (int person = 0; person < groupSize; person++) {
            int birthday = drawBirthday(random, cumulativeDayProbabilities);
            if (observedBirthdays[birthday]) {
                return true;
            }
            observedBirthdays[birthday] = true;
        }
        return false;
    }

    /**
     * Draws one birthday from a cumulative probability distribution.
     *
     * @param random source of randomness
     * @param cumulativeDayProbabilities cumulative distribution over 365 birthdays
     * @return day index from 0 through 364
     */
    public static int drawBirthday(Random random, double... cumulativeDayProbabilities) {
        if (random == null) {
            throw new NullPointerException("Random source must not be null");
        }
        validateCumulativeDayProbabilities(cumulativeDayProbabilities);

        double value = random.nextDouble();
        int index = Arrays.binarySearch(cumulativeDayProbabilities, value);
        if (index >= 0) {
            return index;
        }
        return -index - 1;
    }

    /**
     * Returns the number of people in each trial group.
     *
     * @return group size
     */
    public int getGroupSize() {
        return groupSize;
    }

    /**
     * Returns the requested number of trials.
     *
     * @return trial count
     */
    public int getTrialCount() {
        return trialCount;
    }

    /**
     * Returns the random seed used by this simulation.
     *
     * @return seed value
     */
    public long getSeed() {
        return seed;
    }

    /**
     * Returns the number of trials that have run.
     *
     * @return completed trial count
     */
    public int getCompletedTrials() {
        return completedTrials;
    }

    /**
     * Returns the number of trials in which a collision occurred.
     *
     * @return collision count
     */
    public int getCollisionCount() {
        return collisionCount;
    }

    /**
     * Returns the estimated collision probability.
     *
     * @return collisions divided by completed trials, or {@code 0.0} before
     *         any trials have completed
     */
    public double getEstimatedProbability() {
        if (completedTrials == 0) {
            return 0.0;
        }
        return (double) collisionCount / completedTrials;
    }

    @Override
    protected void initialize() {
        scheduleEvent(new TrialEvent(0.0));
    }

    private void runTrial() {
        if (hasSharedBirthday(groupSize, random, cumulativeDayProbabilities)) {
            collisionCount++;
        }
        completedTrials++;

        if (completedTrials >= trialCount) {
            stop();
        } else {
            scheduleEvent(new TrialEvent(getClock() + 1.0));
        }
    }

    private static void validateMonthWeights(double... monthWeights) {
        if (monthWeights == null) {
            throw new NullPointerException("Month weights must not be null");
        }
        if (monthWeights.length != MONTHS_IN_YEAR) {
            throw new IllegalArgumentException("Expected 12 month weights, found " + monthWeights.length);
        }

        double totalWeight = 0.0;
        for (double weight : monthWeights) {
            if (!Double.isFinite(weight) || weight < 0.0) {
                throw new IllegalArgumentException("Month weights must be finite and nonnegative");
            }
            totalWeight += weight;
        }
        if (totalWeight <= 0.0) {
            throw new IllegalArgumentException("At least one month weight must be positive");
        }
    }

    private static void validateCumulativeDayProbabilities(double... cumulativeDayProbabilities) {
        if (cumulativeDayProbabilities == null) {
            throw new NullPointerException("Cumulative probabilities must not be null");
        }
        if (cumulativeDayProbabilities.length != DAYS_IN_YEAR) {
            throw new IllegalArgumentException(
                "Expected 365 cumulative probabilities, found " + cumulativeDayProbabilities.length);
        }

        double previous = 0.0;
        for (double probability : cumulativeDayProbabilities) {
            if (!Double.isFinite(probability) || probability < previous || probability > 1.0) {
                throw new IllegalArgumentException("Cumulative probabilities must be nondecreasing values in [0, 1]");
            }
            previous = probability;
        }
        if (Math.abs(cumulativeDayProbabilities[DAYS_IN_YEAR - 1] - 1.0) > 1e-12) {
            throw new IllegalArgumentException("Last cumulative probability must equal 1.0");
        }
    }

    private static int parseIntArg(String[] args, int index, int defaultValue, String name) {
        if (args.length <= index) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(name + " must be an integer: " + args[index], ex);
        }
    }

    private static long parseLongArg(String[] args, int index, long defaultValue, String name) {
        if (args.length <= index) {
            return defaultValue;
        }
        try {
            return Long.parseLong(args[index]);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(name + " must be a long integer: " + args[index], ex);
        }
    }

    private class TrialEvent extends Event {

        TrialEvent(double time) {
            super(time);
        }

        @Override
        public void execute(Simulation sim) {
            runTrial();
        }
    }
}
