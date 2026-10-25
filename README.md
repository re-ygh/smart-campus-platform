# Smart Campus Platform — coursework prototype

A university systems-analysis and design project: eight Spring Boot services, a React/TypeScript UI, PostgreSQL, RabbitMQ, Redis, and an API gateway.

## Repository layout

- `services/`: API gateway and auth, booking, marketplace, order, exam, notification, and IoT services
- `web/`: React/Vite interface
- `docs/ARCHITECTURE.md`: original architecture write-up (design intent; some descriptions exceed the implemented behavior)

## Verified in this curated copy

- `npm ci` and `npm run build` pass in `web/`.
- The original Compose file parses with `docker compose config`.
- `docker compose build auth-service` passes. The other seven Java services were not built in this review.

## Running and current limits

The Java services require JDK 17 and Maven, or Docker to build their Dockerfiles. The supplied `services/docker-compose.yml` is retained as coursework source, but it has **not** passed an end-to-end startup test. In particular, its published port mappings use `808x` while the checked-in service configuration uses `908x`, and several services use `localhost` for databases or peers. Configure those addresses before trying to run the full stack in containers.

To inspect or build the UI independently:

```powershell
cd web
npm ci
npm run build
```

The sample database passwords, accounts, and JWT fallback key in the source are public demo values. Do not deploy this configuration to the internet or use it with real data. This repository is a source and architecture portfolio sample; it does not claim production readiness.

The original university folder, generated JARs/classes, UI build output, IDE files, deployment scripts, and the PDF report are not part of this curated copy.
