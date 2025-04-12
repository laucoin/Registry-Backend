# Use an appropriate base image that has Java and Gradle installed
FROM gradle:8.11.0-jdk21-alpine AS temp_build_image

# Set the working directory inside the container
ENV APP_HOME=/usr/app
ENV APP_ARTIFACT_NAME=registry-backend.jar
WORKDIR $APP_HOME

# Copy the application source code to the container
COPY . .

# Build the application using Gradle
RUN gradle build -x test -PtargetName=$APP_ARTIFACT_NAME

# Use an appropriate base image that has Java and Gradle installed
FROM eclipse-temurin:21-alpine

ENV APP_ARTIFACT_NAME=registry-backend.jar
ENV APP_HOME=/usr/app
WORKDIR $APP_HOME

COPY --from=temp_build_image $APP_HOME/build/libs/$APP_ARTIFACT_NAME $APP_HOME/$APP_ARTIFACT_NAME

EXPOSE 8081
CMD java $JAVA_OPTS -jar $APP_ARTIFACT_NAME
