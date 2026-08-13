# Environment variable contract

No production values belong in Git.

| Variable | Required in production | Purpose | Production source |
|---|---:|---|---|
| `DB_URL` | yes | MariaDB JDBC URL | Parameter Store or deployment configuration |
| `DB_USER` | yes | Application DB user | Secrets Manager |
| `DB_PASSWORD` | yes | Application DB password | Secrets Manager |
| `DB_POOL_SIZE` | no | Hikari connection limit | deployment configuration |
| `JPA_DDL_AUTO` | yes | Schema behavior; target is `validate` | deployment configuration |
| `JWT_SECRET` | yes | Admin authentication signing secret | Secrets Manager |
| `CORS_ALLOWED_ORIGINS` | yes | Comma-separated trusted frontend origins | deployment configuration |
| `STORAGE_LOCATION` | yes | Persistent upload directory | deployment configuration |
| `SERVER_PORT` | no | Backend HTTP port; defaults to 8080 | deployment configuration |

Frontend runtime/build contract:

| Variable | Purpose |
|---|---|
| `NEXT_PUBLIC_API_URL` | Empty for same-origin `/api`; explicit localhost URL during split local development |

Changing `JWT_SECRET` invalidates existing login cookies but does not delete users
or content. Database and upload backups must be handled separately.
