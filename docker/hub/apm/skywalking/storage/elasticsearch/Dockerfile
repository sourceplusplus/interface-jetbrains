#
# Source++ Dockerfile
#
# APM:      Apache Skywalking
# Storage:  Elasticsearch
#
FROM blacktop/elasticsearch:6.5.4
LABEL maintainer="github.com/codebrig"

WORKDIR /opt

ENV SOURCE_HOME=/opt/source-core
ARG SOURCE_PLUS_PLUS_VERSION="0.2.0"
ARG SOURCE_PLUS_PLUS_RELEASE="alpha"

ENV SKYWALKING_HOME=/opt/apache-skywalking
ARG SKYWALKING_VERSION="6.1.0"

RUN wget https://github.com/codebrig/source/releases/download/v${SOURCE_PLUS_PLUS_VERSION}-${SOURCE_PLUS_PLUS_RELEASE}/source-core-${SOURCE_PLUS_PLUS_VERSION}.zip && \
    unzip source-core-${SOURCE_PLUS_PLUS_VERSION}.zip && mv source-core-${SOURCE_PLUS_PLUS_VERSION} $SOURCE_HOME

RUN wget https://www-eu.apache.org/dist/incubator/skywalking/${SKYWALKING_VERSION}/apache-skywalking-apm-incubating-${SKYWALKING_VERSION}.zip && \
    unzip apache-skywalking-apm-incubating-${SKYWALKING_VERSION}.zip && mv apache-skywalking-apm-incubating $SKYWALKING_HOME

ENV SOURCE_CONFIG=${SOURCE_HOME}/config/source-core.json

COPY source-core.json ${SOURCE_HOME}/config/source-core.json
COPY source-core.sh /usr/local/bin
RUN chmod +x /usr/local/bin/source-core.sh

COPY application.yml ${SKYWALKING_HOME}/config/application.yml

EXPOSE 8080

RUN chown -R elasticsearch /opt
USER elasticsearch
WORKDIR $SOURCE_HOME
ENTRYPOINT ["source-core.sh"]
