# PulseWatch

A self-hosted uptime and health monitoring platform. Register endpoints, get scheduled health checks, and see status history in a dashboard, with Prometheus metrics and Kubernetes manifests included.

> **Status:** work in progress. See the roadmap below.

## About The Project

PulseWatch lets a user register HTTP and TCP endpoints, checks them on a schedule, and stores the results so uptime and response times can be reviewed over time. It is built as a portfolio project to demonstrate a Java backend together with the DevOps tooling around it: containerization, CI/CD, observability, and Kubernetes.

### Planned Features

- JWT authentication with per-user data isolation
- Monitor management (HTTP and TCP checks, configurable interval and timeout)
- Scheduled health checks with stored result history
- SSRF protection for user-supplied URLs
- React dashboard
- Prometheus metrics and a Grafana dashboard
- CI pipeline: tests, image build, vulnerability scan
- Kubernetes deployment, verified by a smoke test on `kind` in CI

## Built With

- **Backend:** Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA, Flyway
- **Database:** PostgreSQL
- **Frontend:** React (Vite) *(planned)*
- **Observability:** Micrometer, Prometheus, Grafana *(planned)*
- **Infrastructure:** Docker, Docker Compose, GitHub Actions, Kubernetes *(planned)*
- **Testing:** JUnit 5, Mockito, Testcontainers

## Prerequisites

*(to be filled in: Docker, JDK 21, Maven, etc.)*

## How to Run

*(to be filled in as the project grows: clone, environment file, run script, URL and port)*

```
git clone https://github.com/ababitsbence/pulsewatch-uptime-monitor.git
cd pulsewatch-uptime-monitor
```

## Running Tests

*(to be filled in: test command, and a note that Docker must be running for Testcontainers)*

## Architecture

*(diagram to be added in `docs/`)*

## Project Structure

```
pulsewatch-uptime-monitor/
├── backend/        Spring Boot API
├── frontend/       React dashboard (planned)
├── deploy/         Compose and Kubernetes configuration (planned)
├── docs/           Diagrams and screenshots
└── README.md
```

## Roadmap

- [ ] Project skeleton and Docker image
- [ ] JWT authentication
- [ ] Monitor management API
- [ ] Health check engine and scheduler
- [ ] SSRF protection
- [ ] React dashboard
- [ ] Compose stack with Prometheus and Grafana
- [ ] CI pipeline (test, build, scan, push)
- [ ] Kubernetes deployment with kind smoke test

Further ideas are tracked as GitHub Issues.

## License

This project was built as a personal portfolio piece and is not licensed for production use.