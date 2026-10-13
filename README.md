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

- **Backend:** Java 25, Spring Boot 4, Spring Security (JWT), Spring Data JPA, Flyway
- **Database:** PostgreSQL
- **Frontend:** React (Vite) *(planned)*
- **Observability:** Micrometer, Prometheus, Grafana *(planned)*
- **Infrastructure:** Docker, Docker Compose, GitHub Actions, Kubernetes *(planned)*
- **Testing:** JUnit 5, Mockito, Testcontainers

## Prerequisites

- [Docker](https://www.docker.com/), running
- JDK 25, only needed to run the tests or the app outside Docker (Maven is not needed, the wrapper is included)

## How to Run

The API needs PostgreSQL. A single Compose stack is planned; for now, start the two containers by hand.

### 1. Clone the repository

```
git clone https://github.com/ababitsbence/pulsewatch-uptime-monitor.git
cd pulsewatch-uptime-monitor
```

### 2. Build the image and start the containers

```
docker build -t pulsewatch-backend backend
docker network create pulsewatch-net
docker run -d --name pw-db --network pulsewatch-net -e POSTGRES_USER=pulsewatch -e POSTGRES_PASSWORD=pulsewatch -e POSTGRES_DB=pulsewatch postgres:17-alpine
docker run --rm --network pulsewatch-net -p 8081:8081 -e DB_URL=jdbc:postgresql://pw-db:5432/pulsewatch pulsewatch-backend
```

### 3. Check that it is running

In a second terminal:

```
curl.exe http://localhost:8081/actuator/health
```

On macOS/Linux, use `curl` instead of `curl.exe`.

Expected response: `{"groups":["liveness","readiness"],"status":"UP"}`

### 4. Clean up

```
docker rm -f pw-db
docker network rm pulsewatch-net
```

## Running Tests

Docker must be running, because the tests start a PostgreSQL container with Testcontainers.

```
cd backend
./mvnw test        # Windows PowerShell: .\mvnw.cmd test
```

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

- [x] Project skeleton and Docker image
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