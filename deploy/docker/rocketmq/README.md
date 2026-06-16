# RocketMQ 本地开发集群（传统直连模式）

## 架构概览

```
应用 → NameServer（查询路由）→ Broker（直连读写）

拓扑：
- 2 NameServer（namesrv-1 / namesrv-2）
- 2 主从 Broker 对
  - broker-group1: broker-master1 + broker-slave1
  - broker-group2: broker-master2 + broker-slave2
- 1 Dashboard（Web 控制台）
```

## 网络模式

使用 **host 网络模式**，所有 Broker 容器直接使用宿主机网络：
- Broker 注册地址：`127.0.0.1`
- 应用可直接连接 Broker，无需端口映射

## 端口分配

| 服务 | 端口 | 说明 |
|-----|------|------|
| namesrv-1 | 9876 | NameServer 主节点 |
| namesrv-2 | 9877 | NameServer 备节点（容器内9876，映射到宿主机9877） |
| broker-master1 | 10911, 10912 | Broker 组 1 主节点（Remoting + HA） |
| broker-slave1 | 10921, 10922 | Broker 组 1 从节点 |
| broker-master2 | 10931, 10932 | Broker 组 2 主节点 |
| broker-slave2 | 10941, 10942 | Broker 组 2 从节点 |
| dashboard | 8082 | Web 控制台 |

## 应用配置

### application.yml

```yaml
server:
  port: 8090

spring:
  application:
    name: rocketmq-demo

rocketmq:
  # 连接 NameServer，获取路由信息
  name-server: 127.0.0.1:9876
  
  producer:
    group: demo-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 2

logging:
  level:
    com.demo: DEBUG
    org.apache.rocketmq: INFO
```

## 操作命令

### 启动集群

```bash
cd /Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP/deploy/docker/rocketmq
docker compose -f docker-compose-rocketmq.yml up -d
```

### 停止集群

```bash
docker compose -f docker-compose-rocketmq.yml down
```

### 查看日志

```bash
# Master1 日志
docker logs mvp-rocketmq-broker-master1

# Master2 日志
docker logs mvp-rocketmq-broker-master2

# NameServer 日志
docker logs mvp-rocketmq-namesrv-1
```

### 查看集群状态

```bash
docker ps | grep rocketmq
```

## 访问控制台

打开浏览器访问：http://127.0.0.1:8082

## 主从同步说明

- **Master**：负责消息读写，brokerId=0
- **Slave**：从 Master 同步消息，brokerId=1，可承载读流量
- **HA 端口**：Slave 连接 Master 的 HA 端口（10912/10932）同步消息
- **同步模式**：ASYNC_MASTER（异步同步，性能优先）

## 与 Proxy 模式的区别

| 特性 | 传统模式（当前） | Proxy 模式 |
|-----|----------------|-----------|
| 应用连接 | 直连 Broker | 通过 Proxy 统一接入 |
| 配置 | `name-server` | `proxy.endpoints` |
| 网络 | host 网络模式 | bridge + 端口映射 |
| 适用场景 | 本地开发 | 生产环境、多租户 |

## 故障排查

### 问题1：应用无法连接 Broker

**检查步骤：**
1. 确认 Broker 注册地址
   ```bash
   docker logs mvp-rocketmq-broker-master1 | grep "boot success"
   ```
   应该看到：`127.0.0.1:10911`

2. 确认应用配置
   ```yaml
   rocketmq:
     name-server: 127.0.0.1:9876
   ```

### 问题2：Slave 无法同步

**检查步骤：**
1. 查看 Slave 日志
   ```bash
   docker logs mvp-rocketmq-broker-slave1
   ```

2. 确认 haMasterAddress 配置正确
   ```bash
   cat config/broker-slave1.conf | grep haMasterAddress
   ```
   应该是：`haMasterAddress=127.0.0.1:10912`

## 生产环境建议

1. **关闭自动创建 Topic**
   ```properties
   autoCreateTopicEnable=false
   ```

2. **使用同步刷盘**
   ```properties
   flushDiskType=SYNC_FLUSH
   ```

3. **使用同步主从**
   ```properties
   brokerRole=SYNC_MASTER
   ```

4. **增加资源配置**
   ```yaml
   environment:
     JAVA_OPT_EXT: "-Xms2g -Xmx2g -Xmn1g"
   ```
