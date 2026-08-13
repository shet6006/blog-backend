# Current production architecture

Observed on 2026-08-13. Secrets are intentionally omitted.

## Request path

`kimdongwon.me` and `www.kimdongwon.me` are managed in Gabia DNS and point to
CloudFront distribution `E3HT4KMSHBEO8C`. CloudFront terminates TLS with an ACM
certificate in `us-east-1`, then contacts the EC2 origin over HTTP.

Nginx on the EC2 instance proxies `/` to Next.js on port 3000 and `/api/` to
Spring Boot on port 8080.

## AWS resources

- Account: recorded outside this public repository
- Region: `ap-northeast-2` (Seoul)
- One `t3.micro` Amazon Linux 2023 EC2 instance
- One 8 GiB unencrypted gp3 root volume
- Root volume is deleted on instance termination
- No RDS instance
- No Elastic IP
- No EC2 instance role
- SSM Agent is running but the instance is not registered with Systems Manager
- Security group exposes TCP 22 to `0.0.0.0/0`
- TCP 80 is restricted to the CloudFront managed prefix list

## Software and data

- Nginx 1.28
- Node.js 20.19 and Next.js managed by PM2
- Java 17 and Spring Boot managed by PM2
- MariaDB 10.5 on the EC2 host, listening on port 3306
- MariaDB data directory: about 125 MiB at discovery time
- Uploaded avatar: `frontend/public/uploads/avatar_admin.png`
- Application directory: `/home/ec2-user/blogg`
- Root disk usage was 82%, leaving about 1.5 GiB free

## Current deployment

Each repository deploys independently on pushes to `master`. GitHub Actions
stores the EC2 host, user, and SSH private key, connects over SSH, pulls source,
builds on the production instance, and restarts PM2.

This design is retained only until the ECR/OIDC/SSM deployment is verified.
