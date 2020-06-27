#
# Source++ Dockerfile
#
FROM openjdk:11-jre
LABEL maintainer="github.com/sourceplusplus"

ENV SOURCE_HOME=/opt/source-core
ENV SOURCE_CONFIG=${SOURCE_HOME}/config/source-core.json
ARG SOURCE_PLUS_PLUS_VERSION="0.3.0"
ARG SOURCE_PLUS_PLUS_RELEASE="alpha"

RUN wget https://github.com/sourceplusplus/Assistant/releases/download/v${SOURCE_PLUS_PLUS_VERSION}-${SOURCE_PLUS_PLUS_RELEASE}/source-core-${SOURCE_PLUS_PLUS_VERSION}.zip && \
    unzip source-core-${SOURCE_PLUS_PLUS_VERSION}.zip && mv source-core-${SOURCE_PLUS_PLUS_VERSION} $SOURCE_HOME

WORKDIR $SOURCE_HOME

COPY source-core.json ${SOURCE_HOME}/config/source-core.json
COPY source-core.sh /usr/local/bin
RUN chmod +x /usr/local/bin/source-core.sh

EXPOSE 8080

ENTRYPOINT ["source-core.sh"]
