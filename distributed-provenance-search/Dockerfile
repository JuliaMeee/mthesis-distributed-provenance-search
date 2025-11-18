FROM eclipse-temurin:23-jdk as builder

WORKDIR /build

COPY ./gradlew ./gradlew
COPY ./gradle ./gradle
COPY ./build.gradle.kts ./build.gradle.kts
COPY ./settings.gradle.kts ./settings.gradle.kts
COPY ./src ./src

RUN chmod +x ./gradlew
RUN ./gradlew :publishToMavenLocal

RUN mkdir /libs

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