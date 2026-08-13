# Local and production parity

## Differences found

| Concern | Previous local state | Current production | Target shared model |
|---|---|---|---|
| Operating system | Windows, future macOS | Amazon Linux 2023 | Docker images for app and DB |
| Java | Unpinned local JDK | Corretto 17 | Java 17 container image |
| Database | Missing configuration | MariaDB 10.5 on EC2 | MariaDB 10.5 Compose locally; persistent production volume |
| Backend settings | Ignored `application.yml` | Server-only YAML with secrets | Committed placeholder YAML plus environment variables |
| Frontend | Next dev/build | Next server under PM2 | Static Nginx build if dynamic routes can be adapted safely |
| Uploads | Repository-relative | Frontend `public/uploads` plus backend storage configuration | `/data/uploads` persistent volume |
| Secrets | Ad hoc `.env`/server YAML | Server file and GitHub SSH key | local `.env`; AWS Secrets Manager in production |
| Deployment | Manual local commands | Build on EC2 over public SSH | Build once in GitHub Actions, ECR image, deploy by SSM |
| Health | No shared probe | Process presence only | `/actuator/health` plus container health check |
| Tests | Backend failed without production DB config | Not part of deployment | H2 unit/context tests and Compose integration checks |

## Changes already made

- Added a committed, secret-free `application.yml` contract.
- Moved DB, JWT, CORS, port and storage configuration to environment variables.
- Removed production IP/domain hardcoding from backend CORS configuration.
- Added an H2 test configuration so a clean clone can run tests.
- Added a Java 17 non-root Docker image and health check.
- Added MariaDB 10.5 Compose configuration with persistent volumes.

## Remaining differences

- The frontend has two dynamic `[slug]` routes that block a naive Next.js static
  export. These must be adapted and regression-tested before removing the Next
  server.
- Windows OneDrive locks large `node_modules` trees during clean installs. Clone
  development repositories outside OneDrive on both Windows and macOS.
- Production data must be restored into the target MariaDB volume and checked by
  table counts and functional tests.
- Production secrets will be injected from AWS only after the new account is
  connected.
