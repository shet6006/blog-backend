# Migration status

## Completed discovery

- [x] Clone both GitHub repositories
- [x] Connect GitHub with repository administrator access
- [x] Connect only the old AWS account
- [x] Locate the production region and EC2 instance
- [x] Confirm EC2-hosted MariaDB; no RDS
- [x] Confirm CloudFront and Gabia DNS request path
- [x] Confirm public SSH exposure and missing EC2 IAM role
- [x] Inventory runtimes, disk pressure, uploads and deployment workflow
- [x] Establish a passing backend test baseline independent of production secrets

## In progress

- [ ] Build and test Docker images
- [ ] Decide and validate frontend static export implementation
- [ ] Create data backup and verification manifest
- [ ] Create ECR/OIDC/SSM deployment and rollback scripts

## Later approval gates

- [ ] Create billable AWS resources in the new account
- [ ] Stop writes and take final database/upload backup
- [ ] Change Gabia DNS records
- [ ] Remove old GitHub SSH secrets and close port 22
- [ ] Stop or delete old AWS resources

The old account must be logged out before connecting the new account.
