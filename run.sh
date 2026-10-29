#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "Creating .env with generated secrets"
  {
    echo "POSTGRES_PASSWORD=$(openssl rand -hex 16)"
    echo "JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')"
  } > .env
fi

docker compose up --build -d

echo "Waiting for the web app..."
for _ in $(seq 1 90); do
  if curl -fsS http://localhost:8080/ > /dev/null 2>&1; then
    echo "PulseWatch is up: http://localhost:8080  (API: http://localhost:8081)"
    exit 0
  fi
  sleep 2
done

echo "The web app did not become healthy in time. Check: docker compose logs"
exit 1