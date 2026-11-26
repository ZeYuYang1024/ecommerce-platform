FROM eclipse-temurin:17-jdk-jammy AS download
ARG ROCKETMQ_VERSION=5.5.1
ARG ROCKETMQ_SHA512=21555d9bd79792ac89f246566f19f58e300784c59b96289fd0c74db14787861081fb60ebb95cf541bf20a3e508bf3b70feb9596ea3ba6dc5dc88fa6081f00577
RUN set -eux; \
    archive="rocketmq-all-${ROCKETMQ_VERSION}-bin-release"; \
    curl -fsSL --retry 3 "https://downloads.apache.org/rocketmq/${ROCKETMQ_VERSION}/${archive}.zip" -o /tmp/rocketmq.zip \
      || curl -fsSL --retry 3 "https://archive.apache.org/dist/rocketmq/${ROCKETMQ_VERSION}/${archive}.zip" -o /tmp/rocketmq.zip; \
    echo "${ROCKETMQ_SHA512}  /tmp/rocketmq.zip" | sha512sum -c -; \
    cd /tmp; jar xf rocketmq.zip; mv "${archive}" /opt/rocketmq; \
    chmod +x /opt/rocketmq/bin/*.sh /opt/rocketmq/bin/mq*

FROM eclipse-temurin:17-jre-jammy
RUN groupadd --gid 3000 rocketmq && useradd --create-home --uid 3000 --gid rocketmq rocketmq \
    && install -d -o rocketmq -g rocketmq /home/rocketmq/store /home/rocketmq/logs
COPY --from=download /opt/rocketmq /opt/rocketmq
USER rocketmq
WORKDIR /opt/rocketmq/bin
CMD ["sh", "mqnamesrv"]
