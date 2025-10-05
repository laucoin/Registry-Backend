FROM eclipse-temurin:25-jdk

ARG DEBIAN_FRONTEND=noninteractive
ARG USERNAME=vscode
ARG USER_UID=1000
ARG USER_GID=1000

RUN apt update \
    && apt install -y --no-install-recommends bash-completion build-essential ca-certificates curl git jq less nano \
    openssh-client unzip vim zip \
    && rm -rf /var/lib/apt/lists/* \
    && getent group ${USER_GID} || groupadd --gid ${USER_GID} ${USERNAME} \
    && id -u ${USERNAME} &>/dev/null || useradd --uid ${USER_UID} --gid ${USER_GID} -m -s /bin/bash ${USERNAME} \
    && echo "alias ll='ls -alF'" >> /etc/bash.bashrc

ENV JAVA_HOME=/opt/java/openjdk

USER ${USERNAME}
WORKDIR /workspace

EXPOSE 8081 5005