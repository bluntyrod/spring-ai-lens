# spring-ai-lens

[![CI](https://github.com/pandey-prateek/spring-ai-lens/actions/workflows/ci.yml/badge.svg)](https://github.com/pandey-prateek/spring-ai-lens/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

## What it does

Drop `spring-ai-lens` into any Spring Boot app and instantly get:

- **Call history** — every `ChatModel` call captured automatically via AOP
- **Live dashboard** at `/ai-lens` — prompt, response, latency, token usage per call
- **Actuator endpoint** at `/actuator/ai-lens` — JSON report for CI and monitoring hooks
- **Anomaly detection** — latency spikes and token cost outliers flagged automatically
- **Prompt diff viewer** _(coming in v0.4)_ — detect prompt regressions across calls

## Anomaly detection

spring-ai-lens automatically flags calls that look unusual compared to your rolling baseline:

- **Latency spike** — call takes 2x longer than average
- **Token spike** — call uses 2x more tokens than average
- **Both** — flagged together with a combined warning

Anomalies appear as ⚠ badges in the dashboard. No configuration needed — detection kicks in automatically after 3 calls establish a baseline.

## Roadmap

- [x] Phase 1 — AOP interceptor + ring buffer event store
- [x] Phase 2 — embedded dashboard + actuator endpoint
- [x] Phase 3 — anomaly detection for latency and token spikes
- [ ] Phase 4 — prompt diff viewer + regression alerts
- [ ] Phase 5 — OpenTelemetry export + Maven Central publish