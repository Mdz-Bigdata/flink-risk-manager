Build the Flink Risk Control project using Maven.

Ask the user which type of build they want:
1. **Fast** (no tests) — `mvn clean package -pl flink-risk-job -am -DskipTests`
2. **Full build** (all modules, no tests) — `mvn clean package -DskipTests`
3. **Web only** — `mvn clean package -pl flink-risk-web -am -DskipTests`
4. **Frontend only** — `cd frontend && npm run build`

After running, report the build outcome and any failures with their module and error message.
