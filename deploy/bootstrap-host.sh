#!/usr/bin/env bash
set -Eeuo pipefail

dnf install -y docker
systemctl enable --now docker

mkdir -p /opt/blog /etc/blog /srv/blog/mysql /srv/blog/uploads
chmod 700 /etc/blog

# The deployment bundle installs compose.production.yaml and deploy-service.sh
# under /opt/blog. Runtime secrets are written separately from Parameter Store.
touch /etc/blog/runtime.env /etc/blog/images.env
chmod 600 /etc/blog/runtime.env /etc/blog/images.env
