# syntax=docker/dockerfile:1

# ---- Build stage -------------------------------------------------------
# Use an appropriate base image that has Java and Gradle installed
FROM gradle:9.7-jdk25-alpine AS build

# Set the working directory inside the container
ENV APP_HOME=/usr/app
ENV APP_ARTIFACT_NAME=registry-backend.jar
WORKDIR $APP_HOME

# Run as the image's built-in non-root `gradle` user (uid 1000) rather than root: generateJooq
# starts a real ephemeral Postgres (embedded-postgres) to introspect the schema for jOOQ codegen,
# and Postgres refuses to start as root. WORKDIR above creates $APP_HOME as root (0755) before
# this point, so chown it — COPY --chown alone only fixes the copied files, not a directory a
# prior instruction already created, which would otherwise block `gradle` from creating new
# entries (like its project-local .gradle/ cache dir) directly inside it.
RUN chown gradle:gradle "$APP_HOME"
USER gradle
ENV HOME=/home/gradle

# Copy the application source code to the container
COPY --chown=gradle:gradle . .

# Build the application using Gradle. The BuildKit cache mount keeps the Gradle
# dependency + build cache warm across image builds (persists on a stable
# builder; see the CI caching note in the README/PR description). uid/gid pin the
# cache mount to the `gradle` user above so it can actually write to it.
RUN --mount=type=cache,target=/home/gradle/.gradle,uid=1000,gid=1000 \
    gradle build -x test -PtargetName="$APP_ARTIFACT_NAME"

# ---- Runtime stage (distroless) ---------------------------------------
# Use distroless image for the final stage
FROM gcr.io/distroless/java25-debian13:nonroot

# Switch to a non-root user for security
USER nonroot

# Copy the application JAR file from the build stage
COPY --from=build /usr/app/build/libs/registry-backend.jar /registry-backend.jar

# Expose the ports the application listens on (API, then Actuator/Swagger management port)
EXPOSE 8081 8082

# Set the entry point for the container
ENTRYPOINT ["java", "-jar", "/registry-backend.jar"]
