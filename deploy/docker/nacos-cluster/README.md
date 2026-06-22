# Nacos 本地开发集群

## 架构

本目录提供一个 3 节点 Nacos 集群：

| 节点 | 控制台端口 | gRPC client 端口 | 容器名 |
| --- | --- | --- | --- |
| nacos-1 | 8848 | 9848 | `mvp-nacos-1` |
| nacos-2 | 8849 | 9849 | `mvp-nacos-2` |
| nacos-3 | 8850 | 9850 | `mvp-nacos-3` |

控制台入口：

```text
http://127.0.0.1:8848/nacos
```

## 启动前准备

Nacos 集群模式依赖 MySQL。当前 compose 默认连接宿主机 MySQL：

```yaml
MYSQL_SERVICE_HOST: host.docker.internal
MYSQL_SERVICE_PORT: "3306"
MYSQL_SERVICE_DB_NAME: nacos_config
MYSQL_SERVICE_USER: root
MYSQL_SERVICE_PASSWORD: rootroot
```

启动前请先创建数据库并导入脚本：

```bash
mysql -uroot -prootroot -e "CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -prootroot nacos_config < /Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP/deploy/docker/nacos-cluster/config/nacos-cluster.sql
```

## 启动

```bash
cd /Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP/deploy/docker/nacos-cluster
docker compose -f docker-compose-nacos-cluster.yml up -d
```

## 停止

```bash
docker compose -f docker-compose-nacos-cluster.yml down
```

## 查看状态

```bash
docker ps | grep nacos
docker logs -f mvp-nacos-1
docker logs -f mvp-nacos-2
docker logs -f mvp-nacos-3
```

## 应用配置示例

Spring Cloud Alibaba 客户端可以配置任意一个或多个 Nacos 地址：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848,127.0.0.1:8849,127.0.0.1:8850
      config:
        server-addr: 127.0.0.1:8848,127.0.0.1:8849,127.0.0.1:8850
```

## 关键配置说明

| 配置 | 说明 |
| --- | --- |
| `MODE=cluster` | 使用 Nacos 集群模式 |
| `PREFER_HOST_MODE=hostname` | 集群节点之间使用 hostname 互相发现 |
| `NACOS_SERVERS` | 集群成员列表 |
| `SPRING_DATASOURCE_PLATFORM=mysql` | 使用 MySQL 作为配置存储 |
| `MYSQL_SERVICE_*` | MySQL 连接信息 |
| `NACOS_AUTH_ENABLE=false` | 本地开发关闭鉴权 |
| `NACOS_AUTH_TOKEN` | Nacos 鉴权 token，生产环境必须替换 |
| `JVM_XMS/JVM_XMX/JVM_XMN` | JVM 内存配置 |
| `TIME_ZONE/TZ` | 容器时区 |

## 端口说明

| 端口 | 说明 |
| --- | --- |
| `8848` | HTTP 控制台、OpenAPI、客户端注册发现 |
| `9848` | Nacos 2.x gRPC client 端口，默认主端口 + 1000 |
| `9849` | Nacos 2.x gRPC server 端口，默认主端口 + 1001 |
| `7848` | 集群内部 Raft / 选举通信端口 |

## 数据目录

数据和日志挂载到：

```text
/Users/zhongtao/.my_docker/nacos-cluster
```

每个节点独立目录：

```text
nacos-1/logs
nacos-1/data
nacos-2/logs
nacos-2/data
nacos-3/logs
nacos-3/data
```

## 生产环境建议

- 开启 `NACOS_AUTH_ENABLE=true`。
- 替换 `NACOS_AUTH_TOKEN`、`NACOS_AUTH_IDENTITY_KEY`、`NACOS_AUTH_IDENTITY_VALUE`。
- MySQL 使用独立用户，不建议使用 root。
- 根据机器规格增大 JVM 内存。
- 前面增加 Nginx/LB，只暴露统一入口。
