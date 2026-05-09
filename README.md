# Simulation Modeling

A scaffolding project for discrete-event simulation, designed for
college students in an **Algorithms and Algorithm Analysis** course.

## Prerequisites

| Tool | Version |
|------|---------|
| JDK | 17 or higher |
| Apache Ant | 1.10.6 or higher |

Verify with:
```
java -version
ant -version
```

## Project Layout

```
SimulationModeling/
├── build.xml              # Ant build script
├── build.properties       # Configurable build settings
├── lib/                   # JUnit 5 JARs (committed; see resolve target)
├── src/
│   └── main/java/
│       └── simulation/
│           ├── Event.java         # Abstract base for simulation events
│           ├── EventQueue.java    # Min-heap priority queue of events
│           ├── Simulation.java    # Abstract discrete-event engine
│           └── Statistics.java    # Online statistics collector
├── test/
│   └── java/
│       └── simulation/
│           ├── EventTest.java
│           ├── EventQueueTest.java
│           ├── SimulationTest.java
│           └── StatisticsTest.java
└── docs/                  # Generated Javadoc (not committed)
```

## Build Targets

| Target | Description |
|--------|-------------|
| `ant compile` | Compile main sources |
| `ant test` | Compile and run JUnit 5 tests |
| `ant javadoc` | Generate API docs in `docs/` |
| `ant jar` | Package classes into `build/simulation.jar` |
| `ant checkstyle` | Run style checks |
| `ant pmd` | Run PMD source analysis |
| `ant spotbugs` | Run SpotBugs bytecode analysis |
| `ant coverage` | Run tests and generate JaCoCo coverage reports |
| `ant quality` | Run tests, Checkstyle, PMD, SpotBugs, and JaCoCo |
| `ant all` | Full pipeline: clean → quality → javadoc → jar |
| `ant clean` | Remove all generated files |
| `ant resolve` | Download JUnit 5 JARs to `lib/` (needed only if lib/ is absent) |
| `ant resolve-tools` | Download quality-tool distributions to `lib/tools/` |

## Quick Start for Students

```bash
# 1. Clone the repository
git clone https://github.com/MetroCS/SimulationModeling.git
cd SimulationModeling

# 2. Compile and run tests
ant test

# 3. Try the small example simulations
java -cp build/classes simulation.examples.CountdownSimulation
java -cp build/classes simulation.examples.BirthdayCollisionSimulation 23

# 4. Run the full quality pipeline
ant quality

# 5. Build everything (clean + quality + javadoc + jar)
ant all

# 6. Open the generated API documentation
open docs/index.html   # macOS
xdg-open docs/index.html  # Linux
```

Expected example output:

```text
time 0.0: 5
time 1.0: 4
time 2.0: 3
time 3.0: 2
time 4.0: 1
```

If `ant test` succeeds, your development environment is ready.

## Extending the Simulation Framework

Subclass `Simulation` and `Event` to model your problem:

```java
import simulation.Event;
import simulation.Simulation;
import simulation.Statistics;

public class QueueingSimulation extends Simulation {
    private final Statistics waitTime = new Statistics("wait time");
    private int arrivals = 0;

    @Override
    protected void initialize() {
        scheduleEvent(new ArrivalEvent(exponential(1.0), this));
    }

    @Override
    protected boolean shouldStop() {
        return arrivals >= 1000;
    }

    // ... domain-specific methods
}
```

See `src/main/java/simulation/examples/CountdownSimulation.java` for a
complete runnable example of scheduling events.

See `src/main/java/simulation/examples/BirthdayCollisionSimulation.java` for a
Monte Carlo example that estimates the probability of at least two people
sharing a birthday in a group of size `k`. It runs the estimate for 100, 1,000,
and 10,000 trials.

## Suggested First Student Tasks

1. Run `ant test` and confirm all tests pass.
2. Run `simulation.examples.CountdownSimulation`.
3. Run `simulation.examples.BirthdayCollisionSimulation` with a few group sizes.
4. Change an example parameter and rerun the example.
5. Create a new simulation package for your assigned model.
6. Add tests for any behavior you add or change.

## Repository Hygiene

- Commit source files, tests, documentation, and build configuration.
- Do not commit generated `build/` or `docs/` files.
- Keep dependency JARs in `lib/` unless your instructor gives different
  course-specific instructions.
- Run `ant test` before each commit.

## Quality Tools

The project includes enabled Ant targets for:

| Tool | Purpose |
|------|---------|
| **Checkstyle** | Enforce coding style |
| **SpotBugs** | Static bug analysis |
| **PMD** | Source code analysis |
| **JaCoCo** | Test coverage reporting |

Run everything with:

```bash
ant quality
```

The first run downloads tool distributions into `lib/tools/`. Those files are
local build support and are ignored by Git. Reports are written under
`build/reports/`, including JaCoCo HTML coverage at
`build/reports/coverage/index.html`.

## Running Tests Independently

To re-download JUnit 5 JARs (e.g., after a fresh clone where `lib/` is absent):

```bash
ant resolve
```

-----
The bulk of this project was generated by AI tools in response to a small number of human prompts. Human edits were made to the AI-generated content primarily to correct errors, improve adherence to good-practice guidelines, enhance readability, and establish appropriate licensing.

The program is provided “as is” and without warranty of any kind, either expressed or implied:
without even the implied warranty of merchantability and fitness for a particular purpose.

The software is licensed under <a href="https://www.gnu.org/licenses/gpl-3.0.html">GPLv3</a>
<a href="https://www.gnu.org/licenses/gpl-3.0.html"><img src="https://www.gnu.org/graphics/gplv3-with-text-84x42.png" alt="GNU GPLv3 license badge"  align="middle"></a>

This README document is licensed under <a href="https://creativecommons.org/licenses/by-sa/4.0/">CC BY-SA 4.0</a>
<img src="https://mirrors.creativecommons.org/presskit/buttons/88x31/png/by-sa.png" alt="CreativeCommons-BY-SA" width="110" align="middle">
