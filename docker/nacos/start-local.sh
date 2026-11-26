#!/usr/bin/env bash
set -euo pipefail

export NACOS_AUTH_TOKEN="${NACOS_AUTH_TOKEN:-$(openssl rand -base64 32)}"
export NACOS_AUTH_IDENTITY_KEY="${NACOS_AUTH_IDENTITY_KEY:-local-nacos}"
export NACOS_AUTH_IDENTITY_VALUE="${NACOS_AUTH_IDENTITY_VALUE:-$(openssl rand -hex 32)}"

exec bash bin/docker-startup.sh
