#!/usr/bin/env bash
set -Eeuo pipefail

if [[ "${BLOG_RESTORE_CONFIRM:-}" != "NEW_BLOG_EMPTY_DATABASE" ]]; then
  echo "set BLOG_RESTORE_CONFIRM=NEW_BLOG_EMPTY_DATABASE after verifying the new host" >&2
  exit 2
fi

db_dump="${1:?usage: restore-production-data.sh <blog.sql.gz> <uploads.tar.gz>}"
uploads_archive="${2:?usage: restore-production-data.sh <blog.sql.gz> <uploads.tar.gz>}"
runtime_env=/etc/blog/runtime.env

test -s "$db_dump"
test -s "$uploads_archive"
test -f "$runtime_env"

if find /srv/blog/mysql -mindepth 1 -print -quit | grep -q .; then
  echo "/srv/blog/mysql is not empty; refusing to overwrite it" >&2
  exit 3
fi

read_env() {
  local key="$1"
  local value
  value="$(sed -n "s/^${key}=//p" "$runtime_env" | tail -1 | tr -d '\r')"
  [[ -n "$value" ]] || { echo "missing $key in $runtime_env" >&2; exit 4; }
  printf '%s' "$value"
}

db_name="$(read_env DB_NAME)"
db_user="$(read_env DB_USER)"
db_password="$(read_env DB_PASSWORD)"
db_root_password="$(read_env DB_ROOT_PASSWORD)"
container=blog-db-restore

[[ "$db_name" =~ ^[A-Za-z0-9_]+$ ]] || { echo "invalid DB_NAME" >&2; exit 5; }

cleanup() {
  docker rm --force "$container" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker run --detach --name "$container" \
  -e MARIADB_DATABASE="$db_name" \
  -e MARIADB_USER="$db_user" \
  -e MARIADB_PASSWORD="$db_password" \
  -e MARIADB_ROOT_PASSWORD="$db_root_password" \
  -v /srv/blog/mysql:/var/lib/mysql \
  mariadb:10.5 >/dev/null

for _ in $(seq 1 60); do
  if docker exec "$container" healthcheck.sh --connect --innodb_initialized >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
docker exec "$container" healthcheck.sh --connect --innodb_initialized >/dev/null

gzip -dc "$db_dump" \
  | docker exec -i -e MARIADB_PWD="$db_root_password" "$container" \
      mariadb --user=root "$db_name"

install -d -m 755 /srv/blog/uploads
tar -xzf "$uploads_archive" -C /srv/blog/uploads

table_count="$(docker exec -e MARIADB_PWD="$db_root_password" "$container" \
  mariadb --batch --skip-column-names --user=root \
  -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${db_name}'")"
upload_count="$(find /srv/blog/uploads -type f | wc -l | tr -d ' ')"

echo "restore completed: tables=$table_count uploads=$upload_count"
