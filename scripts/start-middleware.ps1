# ============================================================
# E-Commerce Platform — 中间件启动脚本 (Windows PowerShell)
# 用法: .\scripts\start-middleware.ps1
# ============================================================
$ErrorActionPreference = "Stop"
$ProjectDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  E-Commerce 中间件启动" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# --------------- Docker Compose ---------------
Write-Host ""
Write-Host "[1/4] 启动 Docker 容器..." -ForegroundColor Yellow
Push-Location $ProjectDir
docker compose up -d
Pop-Location
Write-Host "容器已启动。" -ForegroundColor Green

# --------------- 等待就绪 ---------------
Write-Host ""
Write-Host "[2/4] 等待服务就绪..." -ForegroundColor Yellow

Write-Host -NoNewline "  MySQL ... "
do { Start-Sleep 2 } until (docker exec ecommerce-mysql mysqladmin ping -uroot -proot --silent 2>$null)
Write-Host "OK" -ForegroundColor Green

Write-Host -NoNewline "  Redis ... "
do { Start-Sleep 1 } until ((docker exec ecommerce-redis redis-cli -a root ping 2>$null) -match "PONG")
Write-Host "OK" -ForegroundColor Green

Write-Host -NoNewline "  Nacos ... "
do { Start-Sleep 3 } until ((Invoke-WebRequest -Uri "http://localhost:8848/nacos/v1/console/health/readiness" -UseBasicParsing 2>$null).Content -match "ok")
Write-Host "OK" -ForegroundColor Green

Write-Host -NoNewline "  RocketMQ NameServer ... "
do { Start-Sleep 3 } until ((docker logs ecommerce-rmq-namesrv 2>$null) -match "Started")
Write-Host "OK" -ForegroundColor Green

Write-Host -NoNewline "  RocketMQ Broker ... "
do { Start-Sleep 3 } until ((docker exec ecommerce-rmq-broker ./mqadmin clusterList -n rocketmq-namesrv:9876 2>$null) -match "BID")
Write-Host "OK" -ForegroundColor Green

Write-Host -NoNewline "  Elasticsearch ... "
do { Start-Sleep 3 } until ((Invoke-WebRequest -Uri "http://localhost:9200/_cluster/health" -UseBasicParsing 2>$null).Content -match '"status"')
Write-Host "OK" -ForegroundColor Green

Write-Host -NoNewline "  MinIO ... "
do { Start-Sleep 2 } until ((Invoke-WebRequest -Uri "http://localhost:9000/minio/health/live" -UseBasicParsing 2>$null).Content -match "OK")
Write-Host "OK" -ForegroundColor Green

# --------------- RocketMQ Topics ---------------
Write-Host ""
Write-Host "[3/4] 初始化 RocketMQ Topics ..." -ForegroundColor Yellow

$Topics = @(
    "order-created:4",
    "order-cancelled:4",
    "order-paid:4",
    "product-created:4",
    "merchant-approved:4"
)

foreach ($entry in $Topics) {
    $parts = $entry -split ":"
    $topic = $parts[0]
    $queues = $parts[1]
    Write-Host -NoNewline "  Creating $topic (queues=$queues)... "
    $result = docker exec ecommerce-rmq-broker ./mqadmin updateTopic `
        -n rocketmq-namesrv:9876 -c DefaultCluster -t $topic `
        -r $queues -w $queues 2>&1
    if ($result -match "success") {
        Write-Host "OK" -ForegroundColor Green
    } else {
        Write-Host "SKIP (may exist)" -ForegroundColor DarkYellow
    }
}

# --------------- Init SQL ---------------
Write-Host ""
Write-Host "[4/4] 初始化数据库 ..." -ForegroundColor Yellow
Get-Content "$ProjectDir\docs\init.sql" | docker exec -i ecommerce-mysql mysql -uroot -proot
Write-Host "数据库初始化完成。" -ForegroundColor Green

# --------------- Done ---------------
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  中间件全部就绪！" -ForegroundColor Green
Write-Host ""
Write-Host "  访问地址:"
Write-Host "    Nacos:    http://localhost:8848/nacos"
Write-Host "    MinIO:    http://localhost:9001"
Write-Host "    ES:       http://localhost:9200"
Write-Host "    Druid:    http://localhost:{服务端口}/druid/sql.html"
Write-Host "    SBA:      http://localhost:8094/admin"
Write-Host ""
Write-Host "  启动后端:   cd <module> && mvn spring-boot:run"
Write-Host "========================================" -ForegroundColor Cyan
