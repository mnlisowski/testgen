FROM maven:3.9-eclipse-temurin-21

WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src

RUN mvn -q -DskipTests package dependency:copy-dependencies

ENTRYPOINT ["java", "-cp", "target/classes:target/dependency/*", "com.mlisows.testgen.cli.Main"]
