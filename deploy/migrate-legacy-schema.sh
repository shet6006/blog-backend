#!/usr/bin/env bash
set -Eeuo pipefail

if [[ "${BLOG_SCHEMA_MIGRATION_CONFIRM:-}" != "NEW_BLOG_DATABASE" ]]; then
  echo "set BLOG_SCHEMA_MIGRATION_CONFIRM=NEW_BLOG_DATABASE after verifying the new host" >&2
  exit 2
fi

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

mysql() {
  docker exec -i -e MYSQL_PWD="$db_root_password" "$container" \
    mariadb --user=root "$db_name" "$@"
}

about_id_type="$(mysql --batch --skip-column-names --execute="
  SELECT DATA_TYPE
  FROM information_schema.columns
  WHERE table_schema='${db_name}' AND table_name='about_page' AND column_name='id';
")"

case "$about_id_type" in
  bigint)
    echo "legacy schema migration already applied"
    exit 0
    ;;
  int) ;;
  *)
    echo "unexpected about_page.id type: $about_id_type" >&2
    exit 5
    ;;
esac

mysql <<'SQL'
ALTER TABLE comments DROP FOREIGN KEY comments_ibfk_1;
ALTER TABLE likes DROP FOREIGN KEY likes_ibfk_1;
ALTER TABLE posts DROP FOREIGN KEY posts_ibfk_1;

ALTER TABLE about_page MODIFY id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE categories MODIFY id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE posts
  MODIFY id BIGINT NOT NULL AUTO_INCREMENT,
  MODIFY category_id BIGINT NULL;
ALTER TABLE comments
  MODIFY id BIGINT NOT NULL AUTO_INCREMENT,
  MODIFY post_id BIGINT NOT NULL;
ALTER TABLE likes
  MODIFY id BIGINT NOT NULL AUTO_INCREMENT,
  MODIFY post_id BIGINT NOT NULL;
ALTER TABLE visitors MODIFY id BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE posts
  ADD CONSTRAINT posts_ibfk_1
  FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;
ALTER TABLE comments
  ADD CONSTRAINT comments_ibfk_1
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE;
ALTER TABLE likes
  ADD CONSTRAINT likes_ibfk_1
  FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE;
SQL

mysql --batch --skip-column-names --execute="
  SELECT CONCAT(TABLE_NAME, '.', COLUMN_NAME, '=', DATA_TYPE)
  FROM information_schema.columns
  WHERE table_schema='${db_name}'
    AND COLUMN_NAME IN ('id', 'post_id', 'category_id')
    AND TABLE_NAME IN ('about_page', 'categories', 'posts', 'comments', 'likes', 'visitors')
  ORDER BY TABLE_NAME, ORDINAL_POSITION;
"
