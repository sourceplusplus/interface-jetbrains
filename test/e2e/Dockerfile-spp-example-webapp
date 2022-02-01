FROM azul/zulu-openjdk-alpine:11

ADD ./example-web-app/build/distributions/example-jvm-boot-1.0-SNAPSHOT.zip /example-jvm-boot-1.0-SNAPSHOT.zip

RUN mkdir -p /tmp/spp-probe/ca
ADD ./config/spp-platform.crt /tmp/spp-probe/ca/ca.crt

ADD spp-probe-*.jar /tmp/spp-probe/spp-probe.jar
ADD ./spp-probe.yml /tmp/spp-probe

ENV SPP_DELETE_PROBE_DIRECTORY_ON_BOOT=false

ENV JAVA_OPTS="-javaagent:/tmp/spp-probe/spp-probe.jar"
ENV SW_AGENT_COLLECTOR_BACKEND_SERVICES=skywalking-oap:11800

RUN unzip *.zip
CMD ["./example-jvm-boot-1.0-SNAPSHOT/bin/example-jvm"]