# Local Dev

```bash
docker compose up -d postgres redis
mvn spring-boot:run
```

Health:

```bash
curl http://localhost:8080/actuator/health
```

Tests:

```bash
mvn test
```

