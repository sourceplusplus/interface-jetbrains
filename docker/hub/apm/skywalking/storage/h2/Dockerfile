#
# Source++ Dockerfile
#
# APM:      Apache Skywalking
# Storage:  H2
#
FROM java:8-jre
LABEL maintainer="github.com/codebrig"

ENV SOURCE_HOME=/opt/source-core
ARG SOURCE_PLUS_PLUS_VERSION="0.2.0"
ARG SOURCE_PLUS_PLUS_RELEASE="alpha"

ENV SKYWALKING_HOME=/opt/apache-skywalking
ARG SKYWALKING_VERSION="6.0.0-GA"

RUN wget https://github.com/codebrig/source/releases/download/v${SOURCE_PLUS_PLUS_VERSION}-${SOURCE_PLUS_PLUS_RELEASE}/source-core-${SOURCE_PLUS_PLUS_VERSION}.zip && \
    unzip source-core-${SOURCE_PLUS_PLUS_VERSION}.zip && mv source-core-${SOURCE_PLUS_PLUS_VERSION} $SOURCE_HOME

RUN wget https://www-eu.apache.org/dist/incubator/skywalking/${SKYWALKING_VERSION}/apache-skywalking-apm-incubating-${SKYWALKING_VERSION}.zip && \
    unzip apache-skywalking-apm-incubating-${SKYWALKING_VERSION}.zip && mv apache-skywalking-apm-incubating $SKYWALKING_HOME

ENV SOURCE_CONFIG=${SOURCE_HOME}/config/source-core.json

WORKDIR $SOURCE_HOME

COPY source-core.json ${SOURCE_HOME}/config/source-core.json
COPY source-core.sh /usr/local/bin
RUN chmod +x /usr/local/bin/source-core.sh

EXPOSE 8080

ENTRYPOINT ["source-core.sh"]
