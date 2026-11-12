#!/usr/bin/env bash
# Creates a local kind cluster and deploys PulseWatch into it, using images built from this checkout.
# Safe to run again: it reuses the cluster and the secret, rebuilds the images and restarts the pods.
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"
cluster="pulsewatch"
ctx="kind-${cluster}"
ns="pulsewatch"

for tool in docker kind kubectl; do
  if ! command -v "$tool" > /dev/null; then
    echo "$tool is not installed or not on the PATH" >&2
    exit 1
  fi
done

cluster_existed=false
if kind get clusters 2> /dev/null | grep -qx "$cluster"; then
  cluster_existed=true
else
  echo "Creating kind cluster '$cluster'"
  kind create cluster --name "$cluster"
fi

echo "Building images"
docker build -t pulsewatch-api:kind "$root/backend"
docker build -t pulsewatch-web:kind "$root/frontend"

echo "Loading images into the cluster"
kind load docker-image pulsewatch-api:kind pulsewatch-web:kind --name "$cluster"

kubectl --context "$ctx" apply -f "$root/deploy/k8s/base/namespace.yaml"

if [ -z "$(kubectl --context "$ctx" -n "$ns" get secret pulsewatch-secrets --ignore-not-found -o name)" ]; then
  echo "Creating the pulsewatch-secrets Secret with generated values"
  kubectl --context "$ctx" -n "$ns" create secret generic pulsewatch-secrets \
    --from-literal=postgres-password="$(openssl rand -hex 16)" \
    --from-literal=jwt-secret="$(openssl rand -base64 48 | tr -d '\n')"
fi

kubectl --context "$ctx" apply -k "$root/deploy/k8s/overlays/kind"

if [ "$cluster_existed" = true ]; then
  # The image tag is always "kind", so a changed image needs a restart to be picked up
  kubectl --context "$ctx" -n "$ns" rollout restart deployment/api deployment/web
fi

echo "Waiting for the workloads to become ready"
kubectl --context "$ctx" -n "$ns" rollout status statefulset/db --timeout=240s
kubectl --context "$ctx" -n "$ns" rollout status deployment/api --timeout=240s
kubectl --context "$ctx" -n "$ns" rollout status deployment/web --timeout=240s

echo
echo "PulseWatch is running in the kind cluster. To open it:"
echo "  kubectl --context $ctx -n $ns port-forward svc/web 8080:8080"
echo "  then open http://localhost:8080"
echo "To remove everything: kind delete cluster --name $cluster"