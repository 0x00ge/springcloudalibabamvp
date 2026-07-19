# Kafka 本地开发集群（KRaft 单节点）

## 架构概览

```
应用 -> Kafka Broker/Controller -> Topic/Partition

拓扑：
- 1 Kafka 节点（broker + controller）
- 1 Kafka UI（Web 控制台）
```

## 端口分配

| 服务 | 端口 | 说明 |
|-----|------|------|
| Kafka | 9092 | 宿主机应用访问地址 |
| Kafka | 29092 | Docker 网络内部访问地址 |
| Kafka UI | 8083 | Web 控制台 |

## 应用配置

`kafka-demo` 默认连接：

```yaml
spring:
  kafka:
    bootstrap-servers: 127.0.0.1:9092
```

## 操作命令

### 启动

```bash
docker network inspect mvp-network >/dev/null 2>&1 || docker network create --subnet 172.30.0.0/16 mvp-network

cd /Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP/deploy/docker/kafka-cluster
docker compose -f docker-compose-kafka-cluster.yml up -d
```

### 停止

```bash
docker compose -f docker-compose-kafka-cluster.yml down
```

### 查看日志

```bash
docker logs mvp-kafka
docker logs mvp-kafka-ui
```

### 查看状态

```bash
docker ps | grep kafka
```

## 访问控制台

打开浏览器访问：http://127.0.0.1:8083

## Demo Topic

`kafka-demo` 启动后会通过 Spring Kafka Admin 自动创建：

| Topic | 说明 |
|------|------|
| demo-order-created | 创建订单消息 |
| demo-order-status-changed | 订单状态变更消息 |
| demo-order-created.DLT | 创建订单死信 Topic |
| demo-order-status-changed.DLT | 状态变更死信 Topic |

## 顺序消息说明

Kafka 只保证单个 partition 内有序。`kafka-demo` 使用 `orderId` 作为消息 key，相同 `orderId` 会进入同一个 partition，因此同一订单的状态变更可以按发送顺序消费。
