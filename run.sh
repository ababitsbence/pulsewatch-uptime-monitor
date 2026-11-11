#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "Creating .env with generated secrets"
  {
    echo "POSTGRES_PASSWORD=$(openssl rand -hex 16)"
    echo "JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')"
    echo "GRAFANA_ADMIN_PASSWORD=$(openssl rand -hex 16)"
  } > .env
elif ! grep -q '^GRAFANA_ADMIN_PASSWORD=' .env; then
  echo "Adding GRAFANA_ADMIN_PASSWORD to the existing .env"
  if [ -n "$(tail -c1 .env)" ]; then echo >> .env; fi
  echo "GRAFANA_ADMIN_PASSWORD=$(openssl rand -hex 16)" >> .env
fi

if ! docker compose up --build -d --wait --wait-timeout 180; then
  echo "The stack did not become healthy in time. Check: docker compose ps, docker compose logs"
  exit 1
fi

echo
echo "PulseWatch is up:"
echo "  Web app:     http://localhost:8080"
echo "  API:         http://localhost:8081"
echo "  Prometheus:  http://localhost:9090"
echo "  Grafana:     http://localhost:3001  (user: admin, password: GRAFANA_ADMIN_PASSWORD in .env)"
