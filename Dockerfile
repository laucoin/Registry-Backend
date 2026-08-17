# syntax=docker/dockerfile:1

# ---- Build stage -------------------------------------------------------
# Use an appropriate base image that has Java and Gradle installed
FROM gradle:9.7.0-jdk25-alpine AS build

# Set the working directory inside the container
ENV APP_HOME=/usr/app
ENV APP_ARTIFACT_NAME=registry-backend.jar
WORKDIR $APP_HOME

# Copy the application source code to the container
COPY . .

# Build the application using Gradle. The BuildKit cache mount keeps the Gradle
# dependency + build cache warm across image builds (persists on a stable
# builder; see the CI caching note in the README/PR description).
RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle build -x test -PtargetName="$APP_ARTIFACT_NAME"

# ---- Runtime stage (distroless) ---------------------------------------
# Use distroless image for the final stage
FROM gcr.io/distroless/java25-debian13:nonroot

# Switch to a non-root user for security
USER nonroot

# Copy the application JAR file from the build stage
COPY --from=build /usr/app/build/libs/registry-backend.jar /registry-backend.jar

# Expose the port the application listens on
EXPOSE 8081

# Set the entry point for the container
ENTRYPOINT ["java", "-jar", "/registry-backend.jar"]
