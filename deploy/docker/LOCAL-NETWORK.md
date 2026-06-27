# Docker 本地网络固定方案

本项目按“Java 服务运行在宿主机，Redis/Nacos/RocketMQ/Nginx 运行在 Docker”来配置。

## 固定规则

- 宿主机 Java 服务访问 Docker 中间件时，统一使用 `127.0.0.1` 和端口映射。
- Docker 容器访问宿主机服务时，统一使用 `host.docker.internal`。
- 不使用家庭、公司或公共网络里的局域网 IP，例如 `192.168.x.x`。
- `mvp-network` 统一由 `redis-cluster/init-redis-cluster.sh` 创建并校验为 `172.19.0.0/16`。
- Redis 节点固定使用 `172.19.0.31-36`，避免容器重建后 `nodes.conf` 指向旧 IP。
- Nacos、RocketMQ、Nginx 都复用 external `mvp-network`，不再各自随机创建网络。

## 推荐启动顺序

```bash
cd /Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP/deploy/docker/redis-cluster
./init-redis-cluster.sh

cd /Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP/deploy/docker/nacos-cluster
docker compose -f docker-compose-nacos-cluster.yml up -d

cd /Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP/deploy/docker/rocketmq-cluster
docker compose -f docker-compose-rocketmq-cluster.yml up -d

cd /Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP/deploy/docker/nginx
docker compose -f docker-compose-nginx.yml up -d
```

## 需要同步到宿主机挂载目录的文件

当前 compose 中部分配置从 `/Users/zhongtao/.my_docker/...` 挂载。修改项目文件后，需要把这些文件同步到对应宿主机目录：

| 项目文件 | 宿主机挂载文件 |
| --- | --- |
| `deploy/docker/rocketmq-cluster/config/broker-master1.conf` | `/Users/zhongtao/.my_docker/rocketmq-cluster/config/broker-master1.conf` |
| `deploy/docker/rocketmq-cluster/config/broker-master2.conf` | `/Users/zhongtao/.my_docker/rocketmq-cluster/config/broker-master2.conf` |
| `deploy/docker/rocketmq-cluster/config/broker-slave1.conf` | `/Users/zhongtao/.my_docker/rocketmq-cluster/config/broker-slave1.conf` |
| `deploy/docker/rocketmq-cluster/config/broker-slave2.conf` | `/Users/zhongtao/.my_docker/rocketmq-cluster/config/broker-slave2.conf` |
| `deploy/docker/nginx/config/nginx.conf` | `/Users/zhongtao/.my_docker/nginx/nginx.conf` |

同步后重启对应容器，让新配置生效。
