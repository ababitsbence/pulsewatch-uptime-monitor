# PulseWatch

A self-hosted uptime and health monitoring platform. Register endpoints, get scheduled health checks, and see status history in a dashboard, with Prometheus metrics and Kubernetes manifests included.

> **Status:** work in progress. See the roadmap below.

## About The Project

PulseWatch lets a user register HTTP and TCP endpoints, checks them on a schedule, and stores the results so uptime and response times can be reviewed over time. It is built as a portfolio project to demonstrate a Java backend together with the DevOps tooling around it: containerization, CI/CD, observability, and Kubernetes.

### Features

- [x] JWT authentication (per-user data isolation arrives with the monitor API)
- [ ] Monitor management (HTTP and TCP checks, configurable interval and timeout)
- [ ] Scheduled health checks with stored result history
- [ ] SSRF protection for user-supplied URLs
- [ ] React dashboard
- [ ] Prometheus metrics and a Grafana dashboard
- [ ] CI pipeline: tests, image build, vulnerability scan
- [ ] Kubernetes deployment, verified by a smoke test on `kind` in CI

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

Generate a signing secret for the JWT tokens (the app refuses to start without one):

```
# Windows PowerShell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); $env:JWT_SECRET = [Convert]::ToBase64String($b)

# macOS / Linux
export JWT_SECRET=$(openssl rand -base64 48)
```

Then build and start the containers:

```
docker build -t pulsewatch-backend backend
docker network create pulsewatch-net
docker run -d --name pw-db --network pulsewatch-net -e POSTGRES_USER=pulsewatch -e POSTGRES_PASSWORD=pulsewatch -e POSTGRES_DB=pulsewatch postgres:17-alpine
docker run --rm --network pulsewatch-net -p 8081:8081 -e DB_URL=jdbc:postgresql://pw-db:5432/pulsewatch -e JWT_SECRET pulsewatch-backend
```

`-e JWT_SECRET` with no value passes the variable from your shell into the container.

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

## API

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | none | Create an account (`email`, `password` of 8-72 characters) |
| POST | `/api/auth/login` | none | Returns a JWT bearer token |
| GET | `/api/users/me` | Bearer token | Returns the current user |

Errors use one JSON format: `{"status": 401, "message": "...", "details": {}}`.

Example (macOS / Linux):

```
curl -X POST http://localhost:8081/api/auth/register -H "Content-Type: application/json" -d '{"email":"me@example.com","password":"supersecret"}'
curl -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"email":"me@example.com","password":"supersecret"}'
curl http://localhost:8081/api/users/me -H "Authorization: Bearer <accessToken>"
```

Windows PowerShell:

```
$body = @{ email = "me@example.com"; password = "supersecret" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/auth/register -ContentType "application/json" -Body $body
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/auth/login -ContentType "application/json" -Body $body
Invoke-RestMethod -Uri http://localhost:8081/api/users/me -Headers @{ Authorization = "Bearer $($login.accessToken)" }
```

## Security Notes

- Passwords are hashed with BCrypt and never returned by the API.
- Login returns the same error for an unknown email and a wrong password, and does the same hashing work for both, to reduce what response time can reveal.
- The JWT signing secret comes from the environment, with no default value.
- Tokens are signed with HS256 and expire after one hour.
- Not implemented yet: rate limiting on login and refresh tokens (tracked as issues).

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
- [x] JWT authentication
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