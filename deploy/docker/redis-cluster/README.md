# Redis Cluster 本地开发集群

## 架构

本目录提供一个 6 节点 Redis Cluster：

| 节点 | 宿主机端口 | Cluster Bus 端口 | 角色 |
| --- | --- | --- | --- |
| redis-7001 | 7001 | 17001 | 初始化后自动分配 |
| redis-7002 | 7002 | 17002 | 初始化后自动分配 |
| redis-7003 | 7003 | 17003 | 初始化后自动分配 |
| redis-7004 | 7004 | 17004 | 初始化后自动分配 |
| redis-7005 | 7005 | 17005 | 初始化后自动分配 |
| redis-7006 | 7006 | 17006 | 初始化后自动分配 |

初始化命令会创建 `3 master + 3 replica`。Redis 会自动分配槽位和主从关系。

镜像版本固定为 `redis:7.2.10`。

## 配置说明

| 配置 | 作用 |
| --- | --- |
| `cluster-enabled yes` | 开启 Redis Cluster 模式 |
| `cluster-config-file nodes.conf` | 保存节点 ID、槽位、主从关系等集群元数据 |
| `cluster-node-timeout 5000` | 节点心跳超时时间，单位毫秒 |
| `appendonly yes` | 开启 AOF 持久化，重启后保留数据 |
| `protected-mode no` | 允许宿主机通过端口映射访问 Redis |
| `bind 0.0.0.0` | 监听容器内所有网卡 |
| `cluster-announce-hostname host.docker.internal` | 让 Redis 对客户端公告宿主机可访问地址 |
| `cluster-announce-port` | 公告 Redis 服务端口 |
| `cluster-announce-bus-port` | 公告 Cluster bus 通信端口 |

`cluster-announce-*` 很关键。Redis Cluster 客户端连接任意节点后，会通过 `CLUSTER SLOTS` 获取所有节点地址。如果返回的是 Docker 容器内 IP，宿主机上的 Java/Redisson 应用通常无法继续连接其他节点。

## 启动并初始化

```bash
cd /Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP/deploy/docker/redis-cluster
chmod +x init-redis-cluster.sh
./init-redis-cluster.sh
```

## 查看集群状态

```bash
docker exec -it mvp-redis-7001 redis-cli -p 7001 cluster info
docker exec -it mvp-redis-7001 redis-cli -p 7001 cluster nodes
```

## 测试读写

```bash
docker exec -it mvp-redis-7001 redis-cli -c -p 7001 set user:1 zhangsan
docker exec -it mvp-redis-7002 redis-cli -c -p 7002 get user:1
```

`-c` 表示启用 cluster 模式，遇到 MOVED 重定向时 redis-cli 会自动跳转到正确节点。

## 停止集群

```bash
docker compose -f docker-compose-redis-cluster.yml down
```

## 清空数据后重新初始化

```bash
docker compose -f docker-compose-redis-cluster.yml down
rm -rf /Users/zhongtao/.my_docker/redis-cluster
./init-redis-cluster.sh
```

## Spring Boot / Redisson 配置示例

Redisson 连接 Redis Cluster 时，需要使用 cluster server 配置。Spring Boot starter 可以这样写：

```yaml
spring:
  data:
    redis:
      cluster:
        nodes:
          - 127.0.0.1:7001
          - 127.0.0.1:7002
          - 127.0.0.1:7003
          - 127.0.0.1:7004
          - 127.0.0.1:7005
          - 127.0.0.1:7006
      timeout: 3000ms
```

如果使用 Redisson 原生 `Config`：

```java
Config config = new Config();
config.useClusterServers()
        .addNodeAddress(
                "redis://127.0.0.1:7001",
                "redis://127.0.0.1:7002",
                "redis://127.0.0.1:7003",
                "redis://127.0.0.1:7004",
                "redis://127.0.0.1:7005",
                "redis://127.0.0.1:7006"
        );
RedissonClient redissonClient = Redisson.create(config);
```

## 注意事项

- 本配置用于本地开发，不设置 Redis 密码。
- 数据持久化在 `/Users/zhongtao/.my_docker/redis-cluster`。
- `cluster-announce-hostname=host.docker.internal` 是为了让宿主机应用能访问集群拓扑中的每个节点。
- 如果在 Linux 上 `host.docker.internal` 不可用，Compose 已配置 `host-gateway` 映射。
