#!/usr/bin/env bash
set -Eeuo pipefail

runtime_env=/etc/blog/runtime.env
container=blog-db-1

test -f "$runtime_env"
docker inspect "$container" >/dev/null

read_env() {
  local key="$1"
  local value
  value="$(sed -n "s/^${key}=//p" "$runtime_env" | tail -1 | tr -d '\r')"
  [[ -n "$value" ]] || { echo "missing $key in $runtime_env" >&2; exit 3; }
  printf '%s' "$value"
}

db_name="$(read_env DB_NAME)"
db_root_password="$(read_env DB_ROOT_PASSWORD)"
[[ "$db_name" =~ ^[A-Za-z0-9_]+$ ]] || { echo "invalid DB_NAME" >&2; exit 4; }

docker exec -i -e MYSQL_PWD="$db_root_password" "$container" \
  mariadb --user=root "$db_name" <<'SQL'
ALTER TABLE posts
  ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(1000) NULL AFTER github_commit_url;
SQL

echo "posts.thumbnail_url is ready"
