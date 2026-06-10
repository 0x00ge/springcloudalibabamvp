# 本地 Docker 开发环境

本目录只在 Docker 中启动：

- Nacos 三节点集群
- RocketMQ 本地开发集群
- Nginx 网关入口

两个 Java `gateway` 实例暂时在宿主机启动，不放进 Docker。

## 请求链路

```text
客户端
  -> 127.0.0.1:8000
  -> Docker: nginx
  -> 宿主机: gateway-1 127.0.0.1:8001
  -> 宿主机: gateway-2 127.0.0.1:8002
  -> Docker: nacos-1/nacos-2/nacos-3
  -> Docker: rocketmq-namesrv-1/rocketmq-namesrv-2
  -> Docker: rocketmq-broker-a/rocketmq-broker-b
```

Nginx 只负责把业务请求转发到两个宿主机 gateway。  
Nacos 和 RocketMQ 不经过 Nginx，宿主机 Java 进程直接连接对应的映射端口。

## 端口说明

| 组件 | 宿主机访问地址 | 容器内地址 | 说明 |
| --- | --- | --- | --- |
| nginx | `127.0.0.1:8000` | `nginx:8000` | 对外统一入口 |
| nacos-1 | `127.0.0.1:8848` | `nacos-1:8848` | Nacos 节点 1 |
| nacos-2 | `127.0.0.1:8849` | `nacos-2:8848` | Nacos 节点 2 |
| nacos-3 | `127.0.0.1:8850` | `nacos-3:8848` | Nacos 节点 3 |
| rocketmq-namesrv-1 | `127.0.0.1:9876` | `rocketmq-namesrv-1:9876` | RocketMQ NameServer 1 |
| rocketmq-namesrv-2 | `127.0.0.1:9877` | `rocketmq-namesrv-2:9876` | RocketMQ NameServer 2 |
| rocketmq-broker-a | `127.0.0.1:10911` | `rocketmq-broker-a:10911` | RocketMQ Broker A |
| rocketmq-broker-b | `127.0.0.1:10921` | `rocketmq-broker-b:10921` | RocketMQ Broker B |
| rocketmq-proxy | `127.0.0.1:8081` | `rocketmq-proxy:8081` | RocketMQ 5.x Proxy |
| rocketmq-dashboard | `127.0.0.1:8088` | `rocketmq-dashboard:8082` | RocketMQ 控制台 |
| gateway-1 | `127.0.0.1:8001` | 宿主机进程 | 手动启动 |
| gateway-2 | `127.0.0.1:8002` | 宿主机进程 | 手动启动 |

## 启动 Docker 中的基础设施

确保宿主机 MySQL 已创建 `nacos_config` 数据库，并导入：

```text
deploy/docker-dev/nacos-cluster.sql
```

启动：

```bash
cd deploy/docker-dev
docker compose up -d
```

查看状态：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f nacos-1
docker compose logs -f nacos-2
docker compose logs -f nacos-3
docker compose logs -f rocketmq-namesrv-1
docker compose logs -f rocketmq-broker-a
docker compose logs -f rocketmq-proxy
docker compose logs -f rocketmq-dashboard
docker compose logs -f nginx
```

访问：

```text
Nginx 健康检查：http://127.0.0.1:8000/nginx-health
Nacos 控制台：http://127.0.0.1:8848/nacos/
RocketMQ 控制台：http://127.0.0.1:8088
```

RocketMQ NameServer 地址：

```text
127.0.0.1:9876;127.0.0.1:9877
```

如果 Java 服务运行在宿主机，配置 RocketMQ 时使用上面的宿主机地址。  
如果 Java 服务以后也放进同一个 Docker 网络，使用容器内地址：

```text
rocketmq-namesrv-1:9876;rocketmq-namesrv-2:9876
```

## 在宿主机启动两个 Gateway

先回到项目根目录打包两个 gateway 模块：

```bash
cd /Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP
mvn -pl mvp-gateway-0,mvp-gateway-1 -am -DskipTests package
```

Nacos 集群地址：

```bash
NACOS_ADDR=127.0.0.1:8848,127.0.0.1:8849,127.0.0.1:8850
```

启动第一个 gateway，也就是 `mvp-gateway-0`，默认端口是 `8001`：

```bash
java -jar mvp-gateway-0/target/mvp-gateway-0-0.0.1-SNAPSHOT.jar \
  --spring.cloud.nacos.server-addr=${NACOS_ADDR} \
  --spring.cloud.nacos.config.server-addr=${NACOS_ADDR} \
  --spring.cloud.nacos.discovery.server-addr=${NACOS_ADDR} \
  --spring.cloud.nacos.discovery.ip=127.0.0.1 \
  --spring.cloud.nacos.discovery.port=8001
```

再开一个终端，启动第二个 gateway，也就是 `mvp-gateway-1`，默认端口是 `8002`：

```bash
java -jar mvp-gateway-1/target/mvp-gateway-1-0.0.1-SNAPSHOT.jar \
  --spring.cloud.nacos.server-addr=${NACOS_ADDR} \
  --spring.cloud.nacos.config.server-addr=${NACOS_ADDR} \
  --spring.cloud.nacos.discovery.server-addr=${NACOS_ADDR} \
  --spring.cloud.nacos.discovery.ip=127.0.0.1 \
  --spring.cloud.nacos.discovery.port=8002
```

访问业务入口：

```bash
curl -i http://127.0.0.1:8000/user/page?page=1\&size=10
```

如果经过 gateway，会看到响应头：

```text
X-Gateway-Route: service-user-route
X-Gateway-Service: service-user-0
```

## 停止

停止 Docker 中的基础设施：

```bash
cd deploy/docker-dev
docker compose down
```

同时删除容器数据卷：

```bash
docker compose down -v
```

宿主机上两个 gateway 进程需要在对应终端按 `Ctrl+C` 停止。
