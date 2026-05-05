FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY jwtjava/.mvn .mvn
COPY jwtjava/mvnw jwtjava/pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -B -DskipTests dependency:go-offline

COPY jwtjava/src src
RUN ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
ENV PORT=8080

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar /app/app.jar"]
