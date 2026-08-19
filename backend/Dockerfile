# Build context is the repository root (see docker-compose.yml) so this stage can pick up the
# shared seed-data/ directory alongside the backend module.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml backend/pom.xml
RUN mvn -q -f backend/pom.xml -DskipTests dependency:go-offline || true
COPY backend/src backend/src
RUN mvn -q -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN addgroup --system app && adduser --system --ingroup app app
WORKDIR /app
COPY --from=build /app/backend/target/platform.jar app.jar
COPY seed-data/customer_orders_seed_v1.csv seed-data/customer_orders_seed_v1.csv
RUN mkdir -p /app/data/datasets && chown -R app:app /app
USER app
EXPOSE 8080
ENV JAVA_OPTS=""
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD curl -sf http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
