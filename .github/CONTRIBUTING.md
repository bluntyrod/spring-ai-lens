# Contributing to spring-ai-lens

## Setup

```bash
git clone https://github.com/pandey-prateek/spring-ai-lens.git
cd spring-ai-lens
mvn install -DskipTests
```

## Running tests

```bash
mvn test
```

## Code style

We use Spotless for formatting. Before submitting a PR:

```bash
mvn spotless:apply
```

## Submitting a PR

1. Open an issue first for anything beyond small fixes
2. Fork the repo and create a branch from `main`
3. Write tests for your changes
4. Run `mvn verify` to confirm tests and formatting pass
5. Submit the PR with a clear description

## Project structure

```
src/main/java/io/ailens/springailens/
├── config/          # autoconfiguration and properties
├── model/           # AiCallEvent and related records
├── actuator/        # /actuator/ai-lens endpoint
├── web/             # embedded dashboard
├── otel/            # OpenTelemetry export
└── util/
    ├── anomaly/     # anomaly detection
    ├── diff/        # prompt diff tracking
    ├── interceptor/ # AOP interceptor
    └── store/       # ring buffer event store
```