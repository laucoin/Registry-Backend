FROM eclipse-temurin:25-jdk

ARG DEBIAN_FRONTEND=noninteractive
ARG USERNAME=vscode
ARG USER_UID=1000
ARG USER_GID=1000

RUN apt-get update
RUN apt-get install -y --no-install-recommends bash-completion git curl unzip zip less jq nano vim ca-certificates openssh-client build-essential
RUN rm -rf /var/lib/apt/lists/*

# Create non-root user with matching UID/GID (Linux host friendly)
RUN getent group ${USER_GID} || groupadd --gid ${USER_GID} ${USERNAME}
RUN id -u ${USERNAME} &>/dev/null || useradd --uid ${USER_UID} --gid ${USER_GID} -m -s /bin/bash ${USERNAME}

RUN echo "alias ll='ls -alF'" >> /etc/bash.bashrc

ENV JAVA_HOME=/opt/java/openjdk

USER ${USERNAME}
WORKDIR /workspace

EXPOSE 8081 5005