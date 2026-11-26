# E-Commerce Platform

[简体中文](README.md) · [Local setup](#local-setup) · [Service ports](#service-ports) · [Explore the code](#explore-the-code)

A Spring Boot 4 and Spring Cloud e-commerce project with a Nuxt storefront, a Vue/Element Plus admin console, a uni-app WeChat Mini Program, and an optional AI knowledge base. The backend uses Nacos for discovery, RocketMQ for asynchronous events, Elasticsearch for product search, and MinIO for file storage. The knowledge module adds Ollama embeddings, Milvus vector search, and DeepSeek chat.

The Java build uses Spring Boot `4.2.0`, Spring Cloud `2026.0.0`, Hutool `5.8.48`, Lombok `1.18.48`, Druid `1.2.28`, and Spring Boot Admin `4.1.5`. The knowledge service uses LangChain4j `1.24.0`. Its BOM selects `1.24.0-beta34` for the Milvus and Easy RAG modules. The exact dependency versions are in the [root POM](pom.xml).

## Local setup

Docker runs the middleware. Java and the two Web development servers run on your host. Follow the steps in order for a fresh installation. The sample credentials and loopback addresses are intended for local development.

### 1. Install the tools and clone the repository

| Tool | What to install |
|---|---|
| Git | Used to clone the repository |
| Docker and Compose v2 | Start Docker Desktop on macOS or Windows. On Linux, start Docker Engine and install the Compose plugin. See the [Docker installation guide](https://docs.docker.com/compose/install/) |
| JDK 21 | Install [Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21) or another JDK 21 distribution. Set `JAVA_HOME` and `PATH` to use it |
| Maven | Install [Maven 3.9 or later](https://maven.apache.org/install.html). The local workflow was verified with Maven 3.9.12 |
| Node.js and npm | [Node.js 24 LTS](https://nodejs.org/en/download) is recommended. If you use Node.js 22, use 22.12 or later. See the [Vite 8 requirements](https://vite.dev/blog/announcing-vite8#node-js-support) |
| Shell | Use Bash on Linux/macOS or PowerShell 7.2+ on Windows. Python 3 is used only for the optional Python commands below |

Allow at least **16 GB of RAM and 20 GB of free disk space** for a full local setup. The first build downloads container images, Maven and npm dependencies, and optionally the embedding model. The middleware combination was verified on Linux/amd64. On an ARM host, check the affected image's architecture support if Docker reports `no matching manifest`.

Clone the project and check the tools. `docker version` should report both client and server details, and `mvn -v` should report Java 21.

```bash
git clone https://github.com/ZeYuYang1024/ecommerce-platform.git
cd ecommerce-platform

docker version
docker compose version
java -version
mvn -v
node -v
npm -v
```

Run commands from the repository root unless a step specifies another directory. In PowerShell, use `curl.exe` if `curl` resolves to an alias, and use `NUL` in place of `/dev/null`.

### 2. Pull images, build MinIO and RocketMQ, and start Docker

Pull the five published images, then build the two images defined in this repository. The RocketMQ NameServer and Broker share a single image.

```bash
docker compose pull mysql redis nacos elasticsearch milvus
docker compose build minio
docker compose build rocketmq-namesrv
docker compose up -d --no-build --pull never
docker compose ps
```

| Compose service | Container | Version | Local ports and purpose |
|---|---|---|---|
| `mysql` | `ecommerce-mysql` | 26.10.0.1 | 3306, application databases, `root / root` |
| `redis` | `ecommerce-redis` | 8.12.0 | 6379, cart and flash-sale cache, password `root` |
| `nacos` | `ecommerce-nacos` | 3.3.0 | 8848 for discovery, 9848 for gRPC, 8849 for the console |
| `rocketmq-namesrv` | `ecommerce-rmq-namesrv` | 5.5.1 | 9876, RocketMQ NameServer |
| `rocketmq-broker` | `ecommerce-rmq-broker` | 5.5.1 | 10911 and 10909, RocketMQ Broker |
| `minio` | `ecommerce-minio` | RELEASE.2025-10-15T17-29-55Z | 9000 for S3, 9001 for the console, `minio / minioadmin` |
| `elasticsearch` | `ecommerce-es` | 9.5.7 | 9200, product search |
| `milvus` | `ecommerce-milvus` | 3.0.6 | 19530 for the vector API, 9091 for health checks, embedded etcd included |

`docker compose ps` should show **eight running containers**. Wait for the services to finish initializing. MySQL should print `1`, Redis should print `PONG`, RocketMQ should display `DefaultCluster`, Nacos should return `"data":"ok"`, and the other HTTP checks should succeed.

```bash
docker compose exec -T mysql mysql -h127.0.0.1 -uroot -proot -Nse "SELECT 1"
docker compose exec -T redis redis-cli -a root ping
docker compose exec -T rocketmq-broker sh mqadmin clusterList -n rocketmq-namesrv:9876
curl --noproxy "*" -fsS http://127.0.0.1:8848/nacos/v3/admin/core/state/readiness
curl --noproxy "*" -fsS http://127.0.0.1:9200/_cluster/health
curl --noproxy "*" -fsS -o /dev/null http://127.0.0.1:9000/minio/health/live
curl --noproxy "*" -fsS http://127.0.0.1:9091/healthz
```

On your first visit to the [Nacos console](http://127.0.0.1:8849/), set a password for the `nacos` administrator. Applications register through ports `8848/9848`. Client authentication is disabled in this local Compose configuration, so setting the console password requires no changes to the Java services. If a check fails, run `docker compose ps -a` and `docker compose logs --tail=100 <service>`.

#### Build and use MinIO

The [MinIO Dockerfile](docker/minio/Dockerfile) downloads the pinned [MinIO Community source release](https://github.com/minio/minio/releases/tag/RELEASE.2025-10-15T17-29-55Z), verifies its SHA-256 checksum, compiles it in a Go build container, and copies the binary to a Debian slim runtime image. Docker handles the build, so a host Go installation is unnecessary. The [RocketMQ Dockerfile](docker/rocketmq/Dockerfile) downloads and verifies the Apache 5.5.1 binary distribution.

To build or inspect MinIO on its own:

```bash
docker compose build minio
docker compose up -d --no-build --pull never minio
docker compose exec -T minio minio --version
docker compose logs --tail=60 minio
```

The S3 endpoint is `http://127.0.0.1:9000`. Sign in to the [console on port 9001](http://127.0.0.1:9001/) with `minio / minioadmin`. See the [file service configuration](ecommerce-file/src/main/resources/application.yml). The first successful image upload creates the `ecommerce` bucket, or you can create that bucket in the console. Try uploading an image of at most 10 MB in the admin product editor, then check for the object in MinIO.

To move the image to another computer, run the first command on the build machine and the last two from the repository root on a target with the **same CPU architecture**:

```bash
docker save -o ecommerce-minio.tar ecommerce-local/minio:RELEASE.2025-10-15T17-29-55Z
docker load -i ecommerce-minio.tar
docker compose up -d --no-build --pull never minio
```

The image tar contains the software. Uploaded objects are stored in the `minio-2025-data` named volume and need a separate backup.

### 3. Initialize MySQL and RocketMQ on the first run

Use these steps with **fresh data volumes**. For an existing installation, back up the data and see the [middleware upgrade guide](docs/middleware-upgrade.md). The checked-in `scripts/start-middleware.sh` and `scripts/start-middleware.ps1` perform only the basic setup. Use the complete sequence here for a fresh installation. If a SQL command fails, inspect the first error before continuing. The logistics and warehouse table creation files cannot be replayed in full against an initialized schema.

**Linux/macOS (Bash)**

```bash
(
  set -e
  db() { docker compose exec -T mysql mysql -uroot -proot --default-character-set=utf8mb4 "$@"; }
  migrations=docs/sql/migrations

  db < docs/init.sql
  db < ecommerce-knowledge/sql/init.sql
  db < "$migrations/2026-05-17-merchant-marketing-tenant-columns.sql"
  db ecommerce_inventory < "$migrations/2026-05-28-inventory-event-log.sql"
  db < "$migrations/2026-06-01-member-init.sql"
  db < "$migrations/2026-06-02-member-phase2a-init.sql"
  db < "$migrations/2026-06-02-order-points-reservation.sql"

  db -e "CREATE DATABASE IF NOT EXISTS ecommerce_logistics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE DATABASE IF NOT EXISTS ecommerce_warehouse CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  db ecommerce_logistics < "$migrations/2026-06-20-logistics-init.sql"
  db ecommerce_logistics < "$migrations/2026-06-20-shipping-template.sql"
  db ecommerce_logistics < docs/sql/seeds/2026-06-20-logistics-provider-seed.sql
  db ecommerce_warehouse < "$migrations/2026-06-20-warehouse-init.sql"

  for schema in ecommerce_inventory ecommerce_order ecommerce_payment ecommerce_logistics ecommerce_warehouse; do
    db "$schema" < "$migrations/2026-05-28-transactional-outbox.sql"
  done
  db -Nse "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME LIKE 'ecommerce%';"
)
```

**Windows (PowerShell 7.2+)**

```powershell
function Import-ProjectSql([string]$Path, [string]$Schema) {
  $dbArgs = @('compose', 'exec', '-T', 'mysql', 'mysql', '-uroot', '-proot', '--default-character-set=utf8mb4')
  if ($Schema) { $dbArgs += $Schema }
  Get-Content -LiteralPath $Path -Raw -Encoding UTF8 | docker @dbArgs
  if ($LASTEXITCODE -ne 0) { throw "SQL import failed: $Path" }
}

$migrations = 'docs/sql/migrations'
Import-ProjectSql 'docs/init.sql'
Import-ProjectSql 'ecommerce-knowledge/sql/init.sql'
Import-ProjectSql "$migrations/2026-05-17-merchant-marketing-tenant-columns.sql"
Import-ProjectSql "$migrations/2026-05-28-inventory-event-log.sql" ecommerce_inventory
Import-ProjectSql "$migrations/2026-06-01-member-init.sql"
Import-ProjectSql "$migrations/2026-06-02-member-phase2a-init.sql"
Import-ProjectSql "$migrations/2026-06-02-order-points-reservation.sql"

docker compose exec -T mysql mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS ecommerce_logistics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE DATABASE IF NOT EXISTS ecommerce_warehouse CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if ($LASTEXITCODE -ne 0) { throw 'Could not create databases' }
Import-ProjectSql "$migrations/2026-06-20-logistics-init.sql" ecommerce_logistics
Import-ProjectSql "$migrations/2026-06-20-shipping-template.sql" ecommerce_logistics
Import-ProjectSql 'docs/sql/seeds/2026-06-20-logistics-provider-seed.sql' ecommerce_logistics
Import-ProjectSql "$migrations/2026-06-20-warehouse-init.sql" ecommerce_warehouse

foreach ($schema in 'ecommerce_inventory', 'ecommerce_order', 'ecommerce_payment', 'ecommerce_logistics', 'ecommerce_warehouse') {
  Import-ProjectSql "$migrations/2026-05-28-transactional-outbox.sql" $schema
}
docker compose exec -T mysql mysql -uroot -proot -Nse "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME LIKE 'ecommerce%';"
```

The final command should list **15 `ecommerce_*` databases**. The initial schema already includes the unique payment order index, so skip `2026-05-29-payment-order-unique.sql` on a fresh install. The member service initializes membership levels at startup. The knowledge SQL creates its database and categories even if you start the knowledge service later.

After `clusterList` shows the Broker, create the 13 application topics. The commands configure four read queues and four write queues per topic.

**Linux/macOS (Bash)**

```bash
(
  set -e
  for topic in order-created order-cancelled order-paid order-refunded product-created merchant-approved review-created seckill-order outbound-shipped shipping-dispatched shipping-exception order-delivered warehouse-stock-changed; do
    docker compose exec -T rocketmq-broker sh mqadmin updateTopic -n rocketmq-namesrv:9876 -c DefaultCluster -t "$topic" -r 4 -w 4
  done
  docker compose exec -T rocketmq-broker sh mqadmin topicList -n rocketmq-namesrv:9876
)
```

**Windows (PowerShell)**

```powershell
$topics = 'order-created order-cancelled order-paid order-refunded product-created merchant-approved review-created seckill-order outbound-shipped shipping-dispatched shipping-exception order-delivered warehouse-stock-changed'.Split(' ')
foreach ($topic in $topics) {
  docker compose exec -T rocketmq-broker sh mqadmin updateTopic -n rocketmq-namesrv:9876 -c DefaultCluster -t $topic -r 4 -w 4
  if ($LASTEXITCODE -ne 0) { throw "Topic creation failed: $topic" }
}
docker compose exec -T rocketmq-broker sh mqadmin topicList -n rocketmq-namesrv:9876
```

### 4. Optional: install Ollama and set up the knowledge base

You can skip this step for the storefront, admin console, and order flow. To use the knowledge module, run Ollama on the **same host as Java**. The Compose project already includes Milvus.

| Platform | Install and start |
|---|---|
| Linux | Follow the [Ollama Linux guide](https://docs.ollama.com/linux). The install command is `curl -fsSL https://ollama.com/install.sh \| sh`. Check the version endpoint below after installation. If the service is unavailable, run `ollama serve` and keep that terminal open |
| macOS | Install and open Ollama using the [Ollama macOS guide](https://docs.ollama.com/macos). Follow the app's prompt to enable the CLI |
| Windows | Install and launch Ollama using the [Ollama Windows guide](https://docs.ollama.com/windows). Open a fresh PowerShell window and leave the background app running |

Download [BGE-M3](https://ollama.com/library/bge-m3), about 1.2 GB. Once the pull finishes, `ollama list` should include `bge-m3`.

```bash
ollama --version
ollama pull bge-m3
ollama list
```

Use the [Ollama Embedding API](https://docs.ollama.com/api/embed) to confirm that it returns **1,024 dimensions**, matching the Milvus collection configured by the application.

**Linux/macOS (Bash, with Python 3 for the dimension check)**

```bash
curl --noproxy "*" -fsS http://127.0.0.1:11434/api/version
curl --noproxy "*" -fsS http://127.0.0.1:11434/api/embed \
  -H 'Content-Type: application/json' \
  -d '{"model":"bge-m3","input":"E-commerce documentation smoke check"}' \
  | python3 -c 'import json,sys; print(len(json.load(sys.stdin)["embeddings"][0]))'
```

**Windows (PowerShell 7.2+)**

```powershell
Invoke-RestMethod -NoProxy http://127.0.0.1:11434/api/version
$body = @{ model = 'bge-m3'; input = 'E-commerce documentation smoke check' } | ConvertTo-Json
$result = Invoke-RestMethod -NoProxy http://127.0.0.1:11434/api/embed -Method Post -ContentType 'application/json' -Body $body
$result.embeddings[0].Count
```

Ollama generates embeddings. DeepSeek provides the default chat model. For chat, set `DEEPSEEK_API_KEY` in the **same terminal that starts Java**, or in the knowledge service's IDEA run configuration. See the [knowledge service configuration](ecommerce-knowledge/src/main/resources/application.yml) and the [DeepSeek API documentation](https://api-docs.deepseek.com/).

**Linux/macOS**

```bash
read -rsp 'DeepSeek API key: ' DEEPSEEK_API_KEY
printf '\n'
export DEEPSEEK_API_KEY
```

**Windows (PowerShell 7.2+)**

```powershell
$env:DEEPSEEK_API_KEY = Read-Host 'DeepSeek API key' -MaskInput
```

With Ollama and Milvus running, you can try ingestion. After starting the knowledge service, sign in as a platform administrator, open the [platform knowledge base](http://localhost:5173/knowledge), create a short document under an existing category, and wait for its chunk count to become positive. After configuring a valid chat key, sign in to the [storefront assistant](http://localhost:3000/knowledge) and ask a question. Keep the key in your environment or local run configuration.

### 5. Build and start the Java services

Build the Maven reactor from the repository root. Each service gets a runnable JAR in its own `target/` directory. The first-run command skips test compilation because the full suite currently has a known compile error in the user module and failures in the knowledge tests.

```bash
mvn '-Dmaven.test.skip=true' clean package
```

Use a dedicated terminal in the repository root. For chat, set the DeepSeek key in that terminal before starting the services. The blocks below start **18 services** by default. Enable the knowledge flag after configuring Ollama to start all **19**. Logs go to `.build-local/`. The commands set logistics to port `8098` so it can run alongside membership on `8096`, register loopback addresses with Nacos, and supply the development Redis password.

**Linux/macOS (Bash)**

```bash
with_knowledge=0   # 1 = enable AI knowledge service
repo="$PWD"
log_dir="$repo/.build-local"
mkdir -p "$log_dir"

modules=(auth user product inventory cart order payment merchant coupon notification file search seckill member warehouse logistics monitor)
if [[ "$with_knowledge" == 1 ]]; then modules+=(knowledge); fi
modules+=(gateway)

backend_pids=()
for module in "${modules[@]}"; do
  name="ecommerce-$module"
  jar="$repo/$name/target/$name-1.0-SNAPSHOT.jar"
  if [[ ! -f "$jar" ]]; then printf 'Missing JAR: %s\n' "$jar"; break; fi
  args=(--server.address=127.0.0.1 --spring.cloud.nacos.discovery.ip=127.0.0.1 --spring.data.redis.password=root)
  if [[ "$module" == logistics ]]; then args+=(--server.port=8098); fi
  (cd "$repo/$name" && exec java -XX:ActiveProcessorCount=4 -Xms64m -Xmx384m -jar "$jar" "${args[@]}") \
    >"$log_dir/$name.log" 2>&1 &
  backend_pids+=("$!")
done
```

**Windows (PowerShell)**

```powershell
$withKnowledge = $false   # $true = enable AI knowledge service
$repo = (Get-Location).Path
$logDir = Join-Path $repo '.build-local'
New-Item -ItemType Directory -Path $logDir -Force | Out-Null
$modules = 'auth user product inventory cart order payment merchant coupon notification file search seckill member warehouse logistics monitor'.Split(' ')
if ($withKnowledge) { $modules += 'knowledge' }
$modules += 'gateway'

$backendProcesses = @()
foreach ($module in $modules) {
  $name = "ecommerce-$module"
  $moduleDir = Join-Path $repo $name
  $jar = Join-Path $moduleDir "target/$name-1.0-SNAPSHOT.jar"
  if (-not (Test-Path -LiteralPath $jar)) { throw "Missing JAR: $jar" }
  $javaArgs = @('-XX:ActiveProcessorCount=4', '-Xms64m', '-Xmx384m', '-jar', ('"{0}"' -f $jar),
    '--server.address=127.0.0.1', '--spring.cloud.nacos.discovery.ip=127.0.0.1',
    '--spring.data.redis.password=root')
  if ($module -eq 'logistics') { $javaArgs += '--server.port=8098' }
  $backendProcesses += Start-Process java -ArgumentList $javaArgs -WorkingDirectory $moduleDir `
    -RedirectStandardOutput (Join-Path $logDir "$name.log") `
    -RedirectStandardError (Join-Path $logDir "$name.err.log") -PassThru
}
```

In IntelliJ IDEA you can instead import the root `pom.xml` and launch each module's `*Application` class with the same program arguments. The logistics service needs `--server.port=8098`. To start it on its own from the repository root:

```bash
java -jar ecommerce-logistics/target/ecommerce-logistics-1.0-SNAPSHOT.jar --server.address=127.0.0.1 --spring.cloud.nacos.discovery.ip=127.0.0.1 --spring.data.redis.password=root --server.port=8098
```

Wait for the gateway and product service, then check both:

```bash
curl --noproxy "*" -fsS http://127.0.0.1:8080/actuator/health
curl --noproxy "*" -fsS "http://127.0.0.1:8080/api/v1/products?page=1&size=5"
```

Health should report `"status":"UP"`. The product endpoint should return application code `200` and sample rows. Gateway health can come up before its downstream services. For a `503`, check `.build-local/ecommerce-product.log` and confirm that the product service appears healthy in Nacos before retrying.

To check an individual service, look up its port in [Service ports](#service-ports), open `http://127.0.0.1:PORT/actuator/health`, and inspect its `.build-local/ecommerce-SERVICE.log`. The PowerShell startup block redirects standard error to a separate `ecommerce-SERVICE.err.log`.

### 6. Start both frontends and sign in

Use **two separate terminals**, each starting from the repository root. `npm ci` installs from the committed lockfiles. Keep the dev servers running.

**Storefront**

```bash
cd ecommerce-web
npm ci
npm run dev
```

**Admin console**

```bash
cd ecommerce-admin
npm ci
npm run dev
```

Both dev servers forward `/api` to the gateway on your host. Use the URL printed by each terminal. The defaults are:

| Page | URL | Demo account |
|---|---|---|
| PC storefront | [http://localhost:3000](http://localhost:3000) | `testuser / test123` |
| Admin console | [http://localhost:5173](http://localhost:5173) | `admin / admin123` |
| Platform knowledge base | [http://localhost:5173/knowledge](http://localhost:5173/knowledge) | Platform administrator |
| Merchant knowledge base | [http://localhost:5173/merchant/knowledge](http://localhost:5173/merchant/knowledge) | Approved merchant administrator |
| Spring Boot Admin | [http://localhost:8094/admin/](http://localhost:8094/admin/) | Local service monitoring |

For a first walkthrough, sign in to the storefront, open a product, and add it to the cart. In the admin console, open the product and inventory pages. Then try image uploads, simulated payment, or the knowledge base. Payment in this setup is for local demonstrations.

### 7. Optional: index the sample products for search

`docs/init.sql` seeds the catalog in MySQL. An SQL import does not emit `product-created`, so an empty Elasticsearch volume can leave search empty or missing an index while the catalog still works. To explore event-driven indexing, create a listed product with a SKU in the admin console and inspect the `ecommerce-search` log.

For a **fresh local search instance**, save the Python 3 snippet below as `index-demo-products.py` and run `python3 index-demo-products.py`, or `py -3 index-demo-products.py` on Windows. This example reads the first page of the product API (up to 100 rows, enough for the default seed) and indexes it in `products`. Matching document IDs in Elasticsearch are overwritten, so use this with demo data.

```python
import json
from urllib.request import ProxyHandler, Request, build_opener

client = build_opener(ProxyHandler({}))
with client.open("http://127.0.0.1:8080/api/v1/products?page=1&size=100", timeout=20) as response:
    page = json.load(response)
assert page["code"] == 200 and page["data"]["records"], page
fields = ("id", "name", "description", "mainImage", "categoryId", "brandId", "merchantId",
          "minPrice", "maxPrice", "status", "avgRating", "reviewCount", "createdAt")
lines = []
for product in page["data"]["records"]:
    doc = {field: product[field] for field in fields if product.get(field) is not None}
    if "createdAt" in doc:
        doc["createdAt"] = doc["createdAt"].replace(" ", "T")
    doc["salesCount"] = 0
    lines.extend((json.dumps({"index": {"_index": "products", "_id": str(doc["id"])}}), json.dumps(doc)))
bulk = ("\n".join(lines) + "\n").encode()
request = Request("http://127.0.0.1:9200/_bulk?refresh=true", data=bulk,
                  headers={"Content-Type": "application/x-ndjson"})
with client.open(request, timeout=30) as response:
    result = json.load(response)
assert not result["errors"], [item for item in result["items"] if item["index"].get("error")]
print("Indexed", len(result["items"]), "products")
```

```bash
curl --noproxy "*" -fsS http://127.0.0.1:9200/products/_count
curl --noproxy "*" -fsS "http://127.0.0.1:8080/api/v1/search?page=1&size=5&sort=price_asc"
```

### 8. Optional: run the WeChat Mini Program

Open [ecommerce-miniprogram](ecommerce-miniprogram/) in HBuilderX. Set your WeChat Mini Program AppID in [manifest.json](ecommerce-miniprogram/manifest.json) and install WeChat Developer Tools. Set the gateway in [utils/api.js](ecommerce-miniprogram/utils/api.js) to an address reachable from the debugger. For H5 development, also change the proxy target in [vite.config.js](ecommerce-miniprogram/vite.config.js). The loopback addresses in this guide are for local development. A physical phone cannot use its own `localhost` to reach services on your computer.

### 9. Stop, restart, and troubleshoot

Press `Ctrl+C` in each frontend terminal. Stop the Java processes from the **same shell session** that started them:

- Bash: `kill "${backend_pids[@]}"`
- PowerShell: `$backendProcesses | Where-Object { -not $_.HasExited } | Stop-Process`

Use `docker compose stop` to stop the middleware. To start again, run `docker compose up -d --no-build --pull never` from the root, check readiness, and restart Java and the frontends. Reuse your existing volumes without importing SQL again. `docker compose down` removes containers and the network while retaining named volumes. `docker compose down -v` removes those volumes and their data.

| Symptom | What to check |
|---|---|
| Images pull, but MinIO or RocketMQ builds time out | Run `docker compose --progress plain build minio` or use `rocketmq-namesrv` as the service. The builder also needs access to GitHub source downloads and the Go module proxy/checksum service, or to Apache downloads. See [Docker build proxy configuration](https://docs.docker.com/engine/cli/proxy/). `HTTP_PROXY` and `HTTPS_PROXY` must be reachable from the build container |
| Elasticsearch reports low `vm.max_map_count` | On a Linux Docker host, run `sudo sysctl -w vm.max_map_count=1048576`. For Docker Desktop or WSL 2, see [Elastic's platform instructions](https://www.elastic.co/docs/deploy-manage/deploy/self-managed/install-elasticsearch-docker-prod) |
| MySQL reports a missing table or column | Check the SQL sequence and schema in step 3, especially logistics, warehouse, membership, and Outbox migrations |
| Redis reports `NOAUTH` | Confirm the Redis password and the Java argument `--spring.data.redis.password=root` |
| A port is already in use | Check for a process from a previous run. Membership uses 8096 and the guide runs logistics on 8098. For frontends, open the URL displayed in the terminal |
| Ollama is unreachable or BGE-M3 is missing | Check `11434/api/version`, run `ollama list`, and confirm that `/api/embed` returns a vector |
| Knowledge returns HTTP 401/403 or chat fails | Sign in with the role appropriate for storefront, platform, or merchant access. For chat failures, also check the DeepSeek key, model settings, and outbound connectivity |

If you have exported proxy URLs reachable from the build container as `HTTP_PROXY` and `HTTPS_PROXY`, retry the build with the following Bash command. Replace `minio` with `rocketmq-namesrv` to retry the RocketMQ build.

```bash
docker compose --progress plain build --build-arg HTTP_PROXY="$HTTP_PROXY" --build-arg HTTPS_PROXY="$HTTPS_PROXY" minio
```

## Service ports

| Port | Service | Port | Service |
|---|---|---|---|
| 8080 | Gateway | 8090 | File |
| 8081 | User | 8091 | Auth |
| 8082 | Product | 8092 | Search |
| 8083 | Inventory | 8093 | Flash sale |
| 8084 | Order | 8094 | Monitor |
| 8085 | Payment | 8095 | Knowledge |
| 8086 | Cart | 8096 | Membership |
| 8087 | Merchant | 8097 | Warehouse |
| 8088 | Coupon | 8098 | Logistics, with the launch override |
| 8089 | Notification | 11434 | Ollama |

## Explore the code

- Start with [the root Maven project](pom.xml) and the [gateway route configuration](ecommerce-gateway/src/main/resources/application.yml) to follow requests from the frontends to each service.
- To follow checkout, read [order](ecommerce-order/), [inventory](ecommerce-inventory/), and [payment](ecommerce-payment/) together with the [Outbox SQL](docs/sql/migrations/2026-05-28-transactional-outbox.sql).
- To follow retrieval, start with the [knowledge service](ecommerce-knowledge/) and its [database schema](ecommerce-knowledge/sql/init.sql).
- For diagrams and business sequences, see the [Chinese architecture overview](README.md#系统架构图). For volume and data migration details, see the [middleware upgrade guide](docs/middleware-upgrade.md).
