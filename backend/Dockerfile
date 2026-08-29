# ---- build ----
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Cache das dependências: copia só o necessário pra baixar o Maven + libs
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

# Código e empacotamento
COPY src/ src/
RUN ./mvnw -q -B clean package -DskipTests

# ---- runtime ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
# Render free = 512 MB. Deixa a JVM respeitar o limite do container.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"

EXPOSE 8082
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
