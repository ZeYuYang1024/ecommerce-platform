# 本地中间件升级

本地 Compose 将 MySQL、Redis、Nacos、MinIO、Elasticsearch 和 Milvus 的新数据放在单独的卷里。对旧环境直接执行 `docker compose up -d` 会启动空卷。先停掉后端服务或暂停写入，保存旧 Compose 文件和原始数据卷，再迁移业务数据。

| 组件 | 迁移方式 |
|---|---|
| MySQL 8.0 → 26.10.0.1 | 导出业务库及其表、触发器、例程和事件，导入新实例。新实例使用 `caching_sha2_password`，请重新建立自定义账户和授权。镜像包版本为 26.10.0.1，SQL `VERSION()` 会显示 26.10.0。 |
| Redis 7.2 → 8.12.0 | 停止写入后执行 `SAVE`，将旧实例的 `dump.rdb` 复制到新数据卷再启动。使用 AOF 的环境还需按原配置迁移 AOF 文件。 |
| Nacos 2.4 → 3.3.0 | 有配置的环境先导出并按官方升级文档迁移命名空间和灰度配置。项目的临时服务实例在后端启动时重新注册。在 `http://127.0.0.1:8849/` 初始化管理员密码。 |
| RocketMQ 5.2 → 5.5.1 | 停止旧 Broker 后保存 `/home/rocketmq/store`，其中包含消息、主题配置及消费偏移。把数据复制到 `rocketmq-5-store`，文件归属为 UID/GID 3000。 |
| MinIO → 2025-10-15 | 停止旧实例后复制整个 `/data`，包括隐藏的 `.minio.sys`。保留原有访问配置。`docker/minio/Dockerfile` 从上游正式标签构建社区版。 |
| Elasticsearch 7.17 → 9.5.7 | 启动一个独立的空白 9.5.7 实例，将旧 `products` 的映射和文档导入新实例。见下方命令。 |
| Milvus 2.4 → 3.0.6 | 空知识库可从新卷启动，由应用重建向量集合。有向量的环境先备份并按上游文档分段升级或导出、重新导入向量，验证后再切换。 |

以下示例假设旧 Elasticsearch 在 9200，新实例临时运行在 19200，且新实例还没有 `products` 索引。

```bash
python3 docker/elasticsearch/migrate-products.py http://127.0.0.1:9200 http://127.0.0.1:19200
```

MySQL 导出示例。将旧实例所有实际使用的业务库列在 `--databases` 后，切换后将该备份导入空的新实例。

```bash
docker exec ecommerce-mysql mysqldump -uroot -proot \
  --single-transaction --routines --events --triggers --no-tablespaces --set-gtid-purged=OFF \
  --databases ecommerce_auth ecommerce_user ecommerce_product > mysql-business-backup.sql
```

恢复应用前，核对新旧数据库行数、商品索引文档数、RocketMQ 主题和消费状态，以及 MinIO 存储桶。回退时使用旧 Compose 和旧卷，并先处理切换后写入新实例的数据。

- [MySQL 升级路径](https://dev.mysql.com/doc/refman/26.10/en/upgrade-paths.html)
- [Nacos 3.3.0 发布说明](https://github.com/alibaba/nacos/releases/tag/3.3.0)
- [Milvus 单机升级文档](https://milvus.io/docs/upgrade_milvus_standalone-docker.md)
