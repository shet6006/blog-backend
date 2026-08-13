# Production deployment harness

The target host contains no Git checkout and performs no application builds.
GitHub Actions builds immutable frontend/backend images, pushes them to ECR with
the commit SHA as tag, and calls SSM Run Command.

SSM invokes:

```bash
sudo /opt/blog/deploy-service.sh backend <immutable-ecr-image-uri>
```

or the equivalent `frontend` command. The script serializes deployments, pulls
only the selected service, waits for its container health check, and restores
the previous image when health does not become ready.

Files on the host:

- `/opt/blog/compose.production.yaml`: non-secret stack definition
- `/opt/blog/deploy-service.sh`: deployment and rollback logic
- `/etc/blog/runtime.env`: mode 600; populated from AWS secure parameters
- `/etc/blog/images.env`: mode 600; current immutable image URIs
- `/srv/blog/mysql`: persistent MariaDB data
- `/srv/blog/uploads`: persistent uploaded files

Port 22 is not required. EC2 receives commands through SSM and pulls ECR images
with its instance role.
