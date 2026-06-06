# Nacos 3 节点集群

本目录用于在本机 Docker 中启动 3 节点 Nacos 集群。

## 结构

```text
127.0.0.1:8848 / 127.0.0.1:9848
        |
        v
    nginx-nacos
        |
        +-- nacos-1
        +-- nacos-2
        +-- nacos-3
        |
        v
host.docker.internal:3306/mvp_config
```

## 启动前准备

当前单机容器 `my-nacos` 已经占用了 `8848/9848/9849`，启动集群前先停掉：

```bash
docker stop my-nacos
```

你的 MySQL 当前是主从复制：

```text
master_server_id_1: 127.0.0.1:3306
slave_server_id_2:  127.0.0.1:3307
```

Nacos 集群要连接可写主库，也就是 `3306`。不要让 Nacos 直接连 `3307` 从库，因为 Nacos 需要写配置、服务元数据、权限数据等。

## 初始化 Nacos 数据库

在主库创建 Nacos MySQL 数据库：

```bash
docker exec master_server_id_1 mysql -uroot -prootroot \
  -e "CREATE DATABASE IF NOT EXISTS mvp_config DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

从 Nacos 镜像里查看 MySQL 初始化脚本位置：

```bash
docker run --rm nacos/nacos-server:v2.5.2 sh -c \
  'find /home/nacos -name "*mysql*.sql"'
```

通常会看到：

```text
/home/nacos/conf/mysql-schema.sql
```

把 Nacos 官方 MySQL 表结构导入主库：

```bash
docker run --rm nacos/nacos-server:v2.5.2 sh -c \
  'cat /home/nacos/conf/mysql-schema.sql' \
  | docker exec -i master_server_id_1 mysql -uroot -prootroot mvp_config
```

检查表是否导入成功：

```bash
docker exec master_server_id_1 mysql -uroot -prootroot \
  -e "USE mvp_config; SHOW TABLES;"
```

能看到 `config_info`、`his_config_info`、`users`、`roles`、`permissions` 等表，就说明 Nacos 数据库初始化完成了。

主库写入后，`mvp_config` 会通过你的 MySQL 主从复制同步到从库。

也可以直接使用本目录脚本完成数据库初始化：

```bash
cd deploy/nacos-cluster
chmod +x nacos-cluster.sh
./nacos-cluster.sh init-db
```

如果启动日志里出现 `Unknown database 'mvp_config'`，说明 Nacos 已经连到 MySQL 了，但是主库里还没有这个数据库，执行上面的 `init-db` 后再启动集群即可。

## 启动

```bash
cd deploy/nacos-cluster
./nacos-cluster.sh start
```

查看状态：

```bash
./nacos-cluster.sh status
./nacos-cluster.sh logs nacos-1
```

访问：

```text
http://127.0.0.1:8848/nacos
```

## 停止

```bash
cd deploy/nacos-cluster
./nacos-cluster.sh stop
```

## 项目配置

因为集群前面用 Nginx 暴露了统一入口，项目里的配置可以继续使用：

```yaml
spring:
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
```

## 注意

- 集群模式必须使用 MySQL，不能只靠内置数据库。
- 3 个节点是最常见的最小生产规模。
- `9848` 是 Nacos 2.x 客户端 gRPC 端口，不能只代理 `8848`。
- 本配置为了兼容本机已有 MySQL，使用 `host.docker.internal:3306` 连接宿主机 MySQL 主库。
- 从库 `3307` 可以作为主库故障后的候选节点；如果主库故障，需要先提升从库为新主库，再把 Nacos 的 MySQL 入口切到新主库。
