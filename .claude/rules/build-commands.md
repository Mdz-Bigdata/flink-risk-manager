# Build Commands

## Maven Commands

```bash
# Fast build, job module only (~30 sec)
mvn clean package -pl flink-risk-job -am -DskipTests

# Full build, all modules (~2 min)
mvn clean package -DskipTests

# Web module only
mvn clean package -pl flink-risk-web -am -DskipTests

# Common module only
mvn clean package -pl flink-risk-common -DskipTests
```

## Frontend Build

```bash
cd frontend
npm install
npm run build
```

## Running Locally

```bash
# 1. Start Docker dependencies
./docker-start.sh start

# 2. Start Flink Job (IDEA or command line)
cd flink-risk-job
mvn exec:java -Dexec.mainClass="com.qinyadan.risk.RiskControlApplication"

# 3. Start Web backend
cd flink-risk-web
mvn spring-boot:run

# 4. Start frontend
cd frontend
npm run dev
```

## Docker Compose

```bash
# Start all services
docker compose up -d

# View logs
docker compose logs -f

# Stop all
docker compose down
```
