# Stage 1: Build the dependency JARs
FROM eclipse-temurin:23-jdk as builder

WORKDIR /build

COPY ./gradlew ./gradlew
COPY ./gradle ./gradle
COPY ./build.gradle.kts ./build.gradle.kts
COPY ./settings.gradle.kts ./settings.gradle.kts
COPY ./src ./src

RUN chmod +x ./gradlew
RUN ./gradlew :publishToMavenLocal

# Copy the built JARs from the local Maven repo to a libs directory
RUN mkdir /libs && \
    cp /root/.m2/repository/cz/muni/fi/cpm/cpm-core/1.0.0/cpm-core-1.0.0.jar /libs/ && \
    cp /root/.m2/repository/cz/muni/fi/cpm/cpm-template/1.0.0/cpm-template-1.0.0.jar /libs/

# Stage 2: Build the final app
FROM eclipse-temurin:23-jdk

WORKDIR /app

COPY --from=builder /libs ./libs
COPY ./gradlew ./gradlew
COPY ./gradle ./gradle
COPY ./build.gradle.kts ./build.gradle.kts
COPY ./settings.gradle.kts ./settings.gradle.kts
COPY ./src ./src

RUN chmod +x ./gradlew
RUN ./gradlew clean build

EXPOSE 8000 5005

CMD ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "/app/build/libs/distributed-provenance-search-1.0-SNAPSHOT.jar"]