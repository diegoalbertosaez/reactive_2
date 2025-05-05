FROM gradle:8.7.0-jdk-21-and-22 as build

WORKDIR /app

COPY build.gradle settings.gradle gradle.properties /app/
COPY src /app/src

RUN mkdir /build
RUN gradle build -Pspring.profiles.active=default --no-daemon

FROM openjdk:21-jdk

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]