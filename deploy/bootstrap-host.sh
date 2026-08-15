#!/usr/bin/env bash
set -Eeuo pipefail

if [[ "${BLOG_BOOTSTRAP_CONFIRM:-}" != "NEW_BLOG_HOST" ]]; then
  echo "set BLOG_BOOTSTRAP_CONFIRM=NEW_BLOG_HOST after verifying the target instance" >&2
  exit 2
fi

aws_region="${AWS_REGION:-ap-northeast-2}"

dnf install -y docker nginx ruby wget
systemctl enable --now docker nginx

if ! docker compose version >/dev/null 2>&1; then
  compose_version="${DOCKER_COMPOSE_VERSION:-v2.40.3}"
  case "$(uname -m)" in
    x86_64) compose_arch=x86_64 ;;
    aarch64) compose_arch=aarch64 ;;
    *) echo "unsupported architecture: $(uname -m)" >&2; exit 3 ;;
  esac
  install -d /usr/local/lib/docker/cli-plugins
  wget -qO /usr/local/lib/docker/cli-plugins/docker-compose \
    "https://github.com/docker/compose/releases/download/${compose_version}/docker-compose-linux-${compose_arch}"
  chmod 755 /usr/local/lib/docker/cli-plugins/docker-compose
fi

if ! systemctl cat codedeploy-agent.service >/dev/null 2>&1; then
  installer="$(mktemp)"
  wget -qO "$installer" \
    "https://aws-codedeploy-${aws_region}.s3.${aws_region}.amazonaws.com/latest/install"
  chmod 700 "$installer"
  "$installer" auto
  rm -f "$installer"
fi
systemctl enable --now codedeploy-agent

mkdir -p /opt/blog /etc/blog /srv/blog/mysql /srv/blog/uploads
chmod 700 /etc/blog

# CodeDeploy installs compose.production.yaml and deploy-service.sh under
# /opt/blog. Write runtime secrets to runtime.env separately; never commit it.
touch /etc/blog/runtime.env /etc/blog/images.env
chmod 600 /etc/blog/runtime.env /etc/blog/images.env

docker compose version
systemctl is-active docker nginx codedeploy-agent
