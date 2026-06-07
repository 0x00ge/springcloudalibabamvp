# Nacos 三节点和 Gateway 双节点集群

本目录只部署：

- Nacos 三节点集群
- Gateway 两个实例
- Nginx Gateway 统一入口

不包含 service-user 和 MySQL 容器。Nacos 使用宿主机现有的 MySQL
`host.docker.internal:3306`。

## 请求链路

```text
客户端 -> nginx-gateway:8000 -> gateway-1/gateway-2
                                      |
                                      +-> 直接连接 nacos-1/2/3
```

Nginx 不代理 Nacos。Gateway 直接使用：

```text
nacos-1:8848,nacos-2:8848,nacos-3:8848
```

## 启动前准备

确保宿主机 MySQL 中已经创建 `nacos_config` 并导入：

```text
deploy/nacos-cluster/nacos-cluster.sql
```

重新打包 Gateway：

```bash
mvn -pl gateway -am -DskipTests package
```

进入部署目录：

```bash
cd deploy/gateway-nacos-cluster
```

Nacos 数据库、Nacos 端口和 Gateway 入口端口已经直接写在 `docker-compose.yml` 中。
如果本机 MySQL 账号密码不是 `root/rootroot`，直接修改 `docker-compose.yml` 里的
`MYSQL_SERVICE_USER` 和 `MYSQL_SERVICE_PASSWORD`。

## 启动

```bash
docker compose up -d
```

查看状态和日志：

```bash
docker compose ps
docker compose logs -f nacos-1
docker compose logs -f gateway-1
docker compose logs -f nginx-gateway
```

## 访问

```text
Gateway 统一入口：http://127.0.0.1:8000
Nginx 健康检查：http://127.0.0.1:8000/nginx-health
Nacos 控制台：http://127.0.0.1:8848/nacos/
```

## 停止

保留日志和 Nacos 本地数据：

```bash
docker compose down
```

同时删除 Compose 数据卷：

```bash
docker compose down -v
```
