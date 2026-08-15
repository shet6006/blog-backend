#!/usr/bin/env bash
set -Eeuo pipefail

new_image="${1:?usage: deploy-service.sh <backend-image-uri>}"
service=backend
image_key=BACKEND_IMAGE

stack_dir=/opt/blog
runtime_env=/etc/blog/runtime.env
images_env=/etc/blog/images.env
compose_file="$stack_dir/compose.production.yaml"

test -f "$runtime_env"
test -f "$images_env"
test -f "$compose_file"

lock_file=/run/lock/blog-deploy.lock
exec 9>"$lock_file"
flock -n 9 || { echo "another deployment is running" >&2; exit 3; }

previous_images="$(mktemp)"
candidate_images="$(mktemp)"
cp "$images_env" "$previous_images"
cp "$images_env" "$candidate_images"
trap 'rm -f "$previous_images" "$candidate_images"' EXIT

if grep -q "^${image_key}=" "$candidate_images"; then
  sed -i "s|^${image_key}=.*|${image_key}=${new_image}|" "$candidate_images"
else
  printf '%s=%s\n' "$image_key" "$new_image" >> "$candidate_images"
fi

compose() {
  docker compose \
    --env-file "$runtime_env" \
    --env-file "$images_env" \
    --file "$compose_file" "$@"
}

install -m 600 "$candidate_images" "$images_env"

compose up --detach db

if compose pull "$service" && compose up --detach --no-deps "$service"; then
  for _ in $(seq 1 30); do
    status="$(compose ps --format json "$service" | head -1)"
    if grep -q '"Health":"healthy"' <<<"$status"; then
      echo "deployment healthy: $service $new_image"
      docker image prune --force --filter 'until=168h' >/dev/null
      exit 0
    fi
    sleep 4
  done
fi

echo "deployment failed; restoring previous image" >&2
install -m 600 "$previous_images" "$images_env"
compose pull "$service" || true
compose up --detach --no-deps "$service"
exit 1
