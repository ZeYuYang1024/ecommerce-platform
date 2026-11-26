FROM golang:1.24.11-bookworm AS build
ARG MINIO_COMMIT=9e49d5e7a648f00e26f2246f4dc28e6b07f8c84a
ARG MINIO_VERSION=2025-10-15T17-29-55Z
ARG MINIO_SHA256=be6d0bd3696c3a13a35f02d3a0280b64319c67918b4501c5c3d87f96d000085c
WORKDIR /src
RUN set -eux; \
    curl -fsSL --retry 3 "https://codeload.github.com/minio/minio/tar.gz/refs/tags/RELEASE.${MINIO_VERSION}" -o /tmp/minio.tar.gz; \
    echo "${MINIO_SHA256}  /tmp/minio.tar.gz" | sha256sum -c -; \
    tar -xzf /tmp/minio.tar.gz --strip-components=1; \
    CGO_ENABLED=0 GOTOOLCHAIN=local go build -mod=readonly -trimpath \
      -ldflags "-s -w -X github.com/minio/minio/cmd.Version=${MINIO_VERSION} -X github.com/minio/minio/cmd.ReleaseTag=RELEASE.${MINIO_VERSION} -X github.com/minio/minio/cmd.CommitID=${MINIO_COMMIT} -X github.com/minio/minio/cmd.CopyrightYear=2025" \
      -o /usr/local/bin/minio .

FROM debian:bookworm-slim
COPY --from=build /usr/local/bin/minio /usr/local/bin/minio
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
ENTRYPOINT ["/usr/local/bin/minio"]
