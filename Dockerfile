# Use an appropriate base image that has Java and Gradle installed
FROM gradle:8.8.0-jdk21-alpine AS temp_build_image

# Set the working directory inside the container
ENV APP_HOME=/usr/app
WORKDIR $APP_HOME

# Copy the Gradle build files to the container
COPY build.gradle.kts $APP_HOME
COPY settings.gradle.kts $APP_HOME
COPY gradlew $APP_HOME
COPY gradle $APP_HOME/gradle

# Copy the application source code to the container
COPY src $APP_HOME/src

# Build the application using Gradle
RUN gradle build -x test

COPY . .

# Use an appropriate base image that has Java and Gradle installed
FROM eclipse-temurin:21-alpine

ENV APP_ARTIFACT_NAME=backend.jar
ENV ARTIFACT_NAME=registry.jar
ENV APP_HOME=/usr/app
WORKDIR $APP_HOME

COPY --from=temp_build_image $APP_HOME/build/libs/$APP_ARTIFACT_NAME $APP_HOME/$ARTIFACT_NAME

EXPOSE 8081
CMD java $JAVA_OPTS -jar $ARTIFACT_NAME
