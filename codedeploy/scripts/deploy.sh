#!/usr/bin/env bash
set -Eeuo pipefail

revision_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
image_uri="$(cat "$revision_dir/image-uri.txt")"
aws_region="$(cat "$revision_dir/aws-region.txt")"
registry="${image_uri%%/*}"

aws ecr get-login-password --region "$aws_region" \
  | docker login --username AWS --password-stdin "$registry"

/opt/blog/migrate-post-thumbnail.sh
/opt/blog/deploy-service.sh "$image_uri"

nginx -t
systemctl reload nginx
