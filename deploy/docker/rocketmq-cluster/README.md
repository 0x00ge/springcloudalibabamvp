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

使用 **bridge 网络 + 端口映射**：
- 容器之间通过服务名互通，例如 `namesrv-1:9876`、`broker-master1:10912`
- Broker 对宿主机注册地址固定为 `127.0.0.1`，宿主机应用通过映射端口直连 Broker
- 不使用家庭、公司或公共网络里的局域网 IP，避免电脑切换网络后 Broker 路由失效
- `mvp-network` 是外部 Docker 网络，先运行 Redis 集群初始化脚本创建并校验固定网段

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
  name-server: 127.0.0.1:9876;127.0.0.1:9877
  access-channel: LOCAL
  
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
cd /Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP/deploy/docker/redis-cluster
./init-redis-cluster.sh

cd /Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP/deploy/docker/rocketmq-cluster
docker compose -f docker-compose-rocketmq-cluster.yml up -d
```

### 停止集群

```bash
docker compose -f docker-compose-rocketmq-cluster.yml down
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
| 网络 | bridge + 端口映射 | bridge + 端口映射 |
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
    name-server: 127.0.0.1:9876;127.0.0.1:9877
    access-channel: LOCAL
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
   应该是：`haMasterAddress=broker-master1:10912`

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

## 配置文件完整同步

本章节将 `rocketmq-cluster` 目录下的 Docker Compose 与 Broker 配置文件内容同步到 Markdown，便于单文件查看、评审和排障。

### 文件清单

| 源文件 | 说明 |
|--------|------|
| `docker-compose-rocketmq-cluster.yml` | RocketMQ 本地开发集群 Docker Compose 编排文件 |
| `config/broker-master1.conf` | `broker-group1` 主节点配置 |
| `config/broker-slave1.conf` | `broker-group1` 从节点配置 |
| `config/broker-master2.conf` | `broker-group2` 主节点配置 |
| `config/broker-slave2.conf` | `broker-group2` 从节点配置 |

### 同步要点

- 集群镜像：Broker / NameServer 使用 `apache/rocketmq:5.3.2`，Dashboard 使用 `apacherocketmq/rocketmq-dashboard:2.1.0`。
- 网络：全部容器加入外部 Docker 网络 `mvp-network`。
- NameServer：`namesrv-1` 映射宿主机 `9876`，`namesrv-2` 容器内仍是 `9876`，映射到宿主机 `9877`。
- Broker 注册地址：所有 Broker 的 `brokerIP1=127.0.0.1`，适配宿主机应用直连本机端口映射。
- 主从关系：
  - `broker-group1`：`broker-master1` + `broker-slave1`，Slave 连接 `broker-master1:10912`。
  - `broker-group2`：`broker-master2` + `broker-slave2`，Slave 连接 `broker-master2:10932`。
- 复制模式：Master 使用 `brokerRole=ASYNC_MASTER`，Slave 使用 `brokerRole=SLAVE`。
- 刷盘模式：全部 Broker 使用 `flushDiskType=ASYNC_FLUSH`。
- 本地开发便利配置：`autoCreateTopicEnable=true`、`autoCreateSubscriptionGroup=true`。
- 数据与日志目录：Compose 挂载到 `/Users/zhongtao/.my_docker/rocketmq-cluster/...`。

### docker-compose-rocketmq-cluster.yml

```yaml
# ============================================================================
# RocketMQ 本地开发集群 - Docker Compose 配置文件（直连模式）
# ============================================================================
#
# 架构说明：
#   采用直连模式：宿主机应用先连 NameServer 拉取路由，再直连 Broker 的 Remoting 端口。
#   应用配置示例（见 service-order-0 application.yml）：
#     rocketmq.name-server: 127.0.0.1:9876;127.0.0.1:9877
#   无需再手写 Broker 地址；Broker 地址由 NameServer 返回（需与 broker.conf 中
#   brokerIP1 / 监听端口 一致，否则宿主机连不上）。
#
# 集群拓扑：
#   ┌─────────────────────────────────────────────────────┐
#   │          应用（宿主机）                               │
#   │    rocketmq.name-server: 127.0.0.1:9876;9877        │
#   └──────────────────┬──────────────────────────────────┘
#                      │
#                      ▼
#           ┌──────────┴───────────┐
#           ▼                      ▼
#      NameServer-1           NameServer-2
#      :9876                  :9877
#           │                      │
#           └──────────┬───────────┘
#                      │
#           ┌──────────┴───────────┐
#           │                      │
#      broker-group1                         broker-group2
#           │                                     │
#       ┌───┴────┐                            ┌───┴────┐
#       │        │                            │        │
#   Master1    Slave1                     Master2    Slave2
#   Remoting   Remoting                   Remoting   Remoting
#   :10911     :10921                     :10931     :10941
#   VIP:10909  VIP:10919                  VIP:10929  VIP:10939
#   HA:10912   HA:10922                   HA:10932   HA:10942
#       │          ▲                          │          ▲
#       │   HA复制  │                          │   HA复制  │
#       └──────────┘                          └──────────┘
#    Slave 主动连 Master.HA                  Slave 主动连 Master.HA
#    (group1: :10922 → :10912)             (group2: :10942 → :10932)
#
#
# --------------------------------------------------------------------------
# 端口使用说明（用途分类）
# --------------------------------------------------------------------------
#
# 1) NameServer 端口（路由发现，应用启动时配置）
#    ┌────────────┬─────────────────┬──────────────────────────────────────┐
#    │ 宿主机端口  │ 容器 / 服务      │ 用途                                 │
#    ├────────────┼─────────────────┼──────────────────────────────────────┤
#    │ 9876       │ namesrv-1:9876  │ NameServer-1 客户端接入端口           │
#    │ 9877       │ namesrv-2:9876  │ NameServer-2（映射到容器内默认 9876） │
#    └────────────┴─────────────────┴──────────────────────────────────────┘
#    · 谁用：Producer / Consumer / Dashboard 的 namesrv 地址列表
#    · 作用：注册与发现 Topic 路由、Broker 存活；本身不存业务消息
#    · 应用只需配 NameServer，不必配下面的 Broker 端口
#
# 2) Broker Remoting 端口（收发消息主通道，NameServer 下发给客户端后直连）
#    ┌────────────┬──────────────────────┬─────────────────────────────────┐
#    │ 宿主机端口  │ 容器 / 服务           │ 用途                            │
#    ├────────────┼──────────────────────┼─────────────────────────────────┤
#    │ 10911      │ broker-master1:10911 │ Master1 主通信（生产/消费/管理）  │
#    │ 10921      │ broker-slave1:10921  │ Slave1  主通信（读、HA 复制相关） │
#    │ 10931      │ broker-master2:10931 │ Master2 主通信                   │
#    │ 10941      │ broker-slave2:10941  │ Slave2  主通信                   │
#    └────────────┴──────────────────────┴─────────────────────────────────┘
#    · 谁用：客户端在拿到路由后自动连接；宿主机直连模式必须映射出来
#    · 若只映射 NameServer、不映射 Remoting，容器内路由返回的端口在宿主机不可达
#
# 3) Broker HA 端口（主从复制；conf 里 haListenPort，主从各自有）
#    ┌────────────┬──────────────────────┬─────────────────────────────────┐
#    │ HA 端口     │ 节点                 │ 角色说明                         │
#    ├────────────┼──────────────────────┼─────────────────────────────────┤
#    │ 10912      │ broker-master1       │ Master1 监听：接受 Slave1 连入    │
#    │ 10922      │ broker-slave1        │ Slave1  本机 HA（conf haListenPort）│
#    │ 10932      │ broker-master2       │ Master2 监听：接受 Slave2 连入    │
#    │ 10942      │ broker-slave2        │ Slave2  本机 HA（conf haListenPort）│
#    └────────────┴──────────────────────┴─────────────────────────────────┘
#    · 复制方向：Slave 主动连接「本 Master 的 haListenPort」做同步（Docker 内网）
#        group1: Slave1 → Master1:10912
#        group2: Slave2 → Master2:10932
#    · 业务应用不直连任何 HA 端口
#    · 本 compose 将 Master/Slave 的 HA 都映射到宿主机，仅为端口对齐、本机探测
#      （如 nc 127.0.0.1 10912）；主从复制不依赖这些宿主机映射，删掉也能复制
#
# 4) Broker VIP 通道端口（默认 listenPort - 2，部分客户端/快速通道）
#    ┌────────────┬──────────────────────┬─────────────────────────────────┐
#    │ 宿主机端口  │ 容器 / 服务           │ 用途                            │
#    ├────────────┼──────────────────────┼─────────────────────────────────┤
#    │ 10909      │ broker-master1:10909 │ Master1 VIP（10911-2）           │
#    │ 10919      │ broker-slave1:10919  │ Slave1  VIP（10921-2）           │
#    │ 10929      │ broker-master2:10929 │ Master2 VIP（10931-2）           │
#    │ 10939      │ broker-slave2:10939  │ Slave2  VIP（10941-2）           │
#    └────────────┴──────────────────────┴─────────────────────────────────┘
#    · 谁用：开启 VIP 通道时的客户端；未开启时映射也非业务必需
#    · 约定：vip 端口 = remoting 端口 - 2
#
# 5) Dashboard（运维控制台）
#    ┌────────────┬──────────────────────┬─────────────────────────────────┐
#    │ 宿主机端口  │ 容器 / 服务           │ 用途                            │
#    ├────────────┼──────────────────────┼─────────────────────────────────┤
#    │ 8082       │ dashboard:8082       │ RocketMQ Dashboard Web UI       │
#    └────────────┴──────────────────────┴─────────────────────────────────┘
#    · 浏览器访问：http://127.0.0.1:8082
#    · 容器内通过 namesrv-1:9876;namesrv-2:9876 连集群（见 dashboard 环境变量）
#
# --------------------------------------------------------------------------
# 端口映射一览（宿主机 → 容器，与下方 services.ports 一致）
# --------------------------------------------------------------------------
#   【必选 · 业务直连】
#   9876  → namesrv-1:9876          NameServer-1
#   9877  → namesrv-2:9876          NameServer-2
#   10911 → broker-master1:10911    Master1 Remoting（生产/消费）
#   10921 → broker-slave1:10921     Slave1  Remoting
#   10931 → broker-master2:10931    Master2 Remoting
#   10941 → broker-slave2:10941     Slave2  Remoting
#   8082  → dashboard:8082          Web 控制台
#
#   【可选 · 非业务必需；映射仅为端口对齐 / 本机探测 / VIP 兼容】
#   10912 → broker-master1:10912    Master1 HA（复制走容器网，不依赖本映射）
#   10922 → broker-slave1:10922     Slave1  HA（同上，非复制对端）
#   10932 → broker-master2:10932    Master2 HA
#   10942 → broker-slave2:10942     Slave2  HA
#   10909 → broker-master1:10909    Master1 VIP（listenPort-2）
#   10919 → broker-slave1:10919     Slave1  VIP
#   10929 → broker-master2:10929    Master2 VIP
#   10939 → broker-slave2:10939     Slave2  VIP
#
# 快速记忆：
#   · 应用配置只记 9876 / 9877；收发消息用 Remoting 10911/10921/10931/10941
#   · HA/VIP 已全部映射，但主从复制与日常业务都不依赖「宿主机上的 HA/VIP」
#   · 复制：容器内 Slave → Master.HA（10912/10932）；删掉 HA 的 ports 一般仍可复制
#
# Web 控制台：
#   http://127.0.0.1:8082
#
# ============================================================================


x-rocketmq-cluster-common: &rocketmq-cluster-common
  image: apache/rocketmq:5.3.2
  restart: unless-stopped
  environment:
    JAVA_OPT_EXT: "-Xms256m -Xmx256m -Xmn128m"
    TZ: Asia/Shanghai
  networks:
    - mvp-network


services:

  namesrv-1:
    <<: *rocketmq-cluster-common
    container_name: mvp-rocketmq-namesrv-1
    hostname: namesrv-1
    command: sh mqnamesrv
    ports:
      - "9876:9876"
    volumes:
      - /Users/zhongtao/.my_docker/rocketmq-cluster/namesrv-1/logs:/home/rocketmq/logs

  namesrv-2:
    <<: *rocketmq-cluster-common
    container_name: mvp-rocketmq-namesrv-2
    hostname: namesrv-2
    command: sh mqnamesrv
    ports:
      - "9877:9876"
    volumes:
      - /Users/zhongtao/.my_docker/rocketmq-cluster/namesrv-2/logs:/home/rocketmq/logs

  broker-master1:
    <<: *rocketmq-cluster-common
    container_name: mvp-rocketmq-broker-master1
    hostname: broker-master1
    depends_on:
      - namesrv-1
      - namesrv-2
    environment:
      JAVA_OPT_EXT: "-Xms512m -Xmx512m -Xmn256m"
      NAMESRV_ADDR: namesrv-1:9876;namesrv-2:9876
      TZ: Asia/Shanghai
    command: sh mqbroker -c /home/rocketmq/conf/broker-master1.conf
    ports:
      # Remoting（listenPort）：宿主机应用直连收发消息，本地直连模式建议保留。
      - "10911:10911"
      # HA（haListenPort）：复制时 Slave 在 Docker 网内连 broker-master1:10912，不依赖本映射。
      # 映射到宿主机：非必须，仅为端口对齐/本机探测（如 nc 127.0.0.1 10912）；业务不连此口。
      - "10912:10912"
      # VIP（listenPort-2）：非必须；仅客户端开启 VIP 通道时使用，便于与 conf 对照。
      - "10909:10909"
    volumes:
      - /Users/zhongtao/.my_docker/rocketmq-cluster/config/broker-master1.conf:/home/rocketmq/conf/broker-master1.conf:ro
      - /Users/zhongtao/.my_docker/rocketmq-cluster/broker-master1/store:/home/rocketmq/store
      - /Users/zhongtao/.my_docker/rocketmq-cluster/broker-master1/logs:/home/rocketmq/logs

  broker-slave1:
    <<: *rocketmq-cluster-common
    container_name: mvp-rocketmq-broker-slave1
    hostname: broker-slave1
    depends_on:
      - namesrv-1
      - namesrv-2
      - broker-master1
    environment:
      JAVA_OPT_EXT: "-Xms512m -Xmx512m -Xmn256m"
      NAMESRV_ADDR: namesrv-1:9876;namesrv-2:9876
      TZ: Asia/Shanghai
    command: sh mqbroker -c /home/rocketmq/conf/broker-slave1.conf
    ports:
      # Remoting：读流量/客户端；宿主机直连时建议保留。
      - "10921:10921"
      # HA（conf haListenPort=10922）：复制方向是本节点 → Master:10912，不依赖本映射。
      # 映射到宿主机：非必须，仅为对齐 conf、本机探测。
      - "10922:10922"
      # VIP（listenPort-2）：非必须，便于对照与 VIP 客户端兼容。
      - "10919:10919"
    volumes:
      - /Users/zhongtao/.my_docker/rocketmq-cluster/config/broker-slave1.conf:/home/rocketmq/conf/broker-slave1.conf:ro
      - /Users/zhongtao/.my_docker/rocketmq-cluster/broker-slave1/store:/home/rocketmq/store
      - /Users/zhongtao/.my_docker/rocketmq-cluster/broker-slave1/logs:/home/rocketmq/logs

  broker-master2:
    <<: *rocketmq-cluster-common
    container_name: mvp-rocketmq-broker-master2
    hostname: broker-master2
    depends_on:
      - namesrv-1
      - namesrv-2
    environment:
      JAVA_OPT_EXT: "-Xms512m -Xmx512m -Xmn256m"
      NAMESRV_ADDR: namesrv-1:9876;namesrv-2:9876
      TZ: Asia/Shanghai
    command: sh mqbroker -c /home/rocketmq/conf/broker-master2.conf
    ports:
      # Remoting：宿主机应用直连收发，本地直连模式建议保留。
      - "10931:10931"
      # HA：复制走 Docker 网 broker-master2:10932，不依赖本映射；宿主机映射非必须（探测/对照）。
      - "10932:10932"
      # VIP：非必须，listenPort-2。
      - "10929:10929"
    volumes:
      - /Users/zhongtao/.my_docker/rocketmq-cluster/config/broker-master2.conf:/home/rocketmq/conf/broker-master2.conf:ro
      - /Users/zhongtao/.my_docker/rocketmq-cluster/broker-master2/store:/home/rocketmq/store
      - /Users/zhongtao/.my_docker/rocketmq-cluster/broker-master2/logs:/home/rocketmq/logs

  broker-slave2:
    <<: *rocketmq-cluster-common
    container_name: mvp-rocketmq-broker-slave2
    hostname: broker-slave2
    depends_on:
      - namesrv-1
      - namesrv-2
      - broker-master2
    environment:
      JAVA_OPT_EXT: "-Xms512m -Xmx512m -Xmn256m"
      NAMESRV_ADDR: namesrv-1:9876;namesrv-2:9876
      TZ: Asia/Shanghai
    command: sh mqbroker -c /home/rocketmq/conf/broker-slave2.conf
    ports:
      # Remoting：读流量/客户端；宿主机直连时建议保留。
      - "10941:10941"
      # HA（conf haListenPort=10942）：复制是本节点 → Master:10932，不依赖本映射；映射非必须。
      - "10942:10942"
      # VIP：非必须，listenPort-2。
      - "10939:10939"
    volumes:
      - /Users/zhongtao/.my_docker/rocketmq-cluster/config/broker-slave2.conf:/home/rocketmq/conf/broker-slave2.conf:ro
      - /Users/zhongtao/.my_docker/rocketmq-cluster/broker-slave2/store:/home/rocketmq/store
      - /Users/zhongtao/.my_docker/rocketmq-cluster/broker-slave2/logs:/home/rocketmq/logs

  dashboard:
    image: apacherocketmq/rocketmq-dashboard:2.1.0
    container_name: mvp-rocketmq-dashboard
    restart: unless-stopped
    depends_on:
      - broker-master1
      - broker-master2
    environment:
      # Dashboard 在容器内通过容器名访问 Broker
      JAVA_OPTS: "-Drocketmq.namesrv.addr=namesrv-1:9876;namesrv-2:9876"
      TZ: Asia/Shanghai
    ports:
      - "8082:8082"
    volumes:
      - /Users/zhongtao/.my_docker/rocketmq-cluster/dashboard/logs:/root/logs
      # 👇 新增以下4个映射，阻止 Tomcat 产生随机匿名卷
      - /Users/zhongtao/.my_docker/rocketmq-cluster/dashboard/webapps:/usr/local/tomcat/webapps
      - /Users/zhongtao/.my_docker/rocketmq-cluster/dashboard/work:/usr/local/tomcat/work
      - /Users/zhongtao/.my_docker/rocketmq-cluster/dashboard/temp:/usr/local/tomcat/temp
      - /Users/zhongtao/.my_docker/rocketmq-cluster/tmp:/tmp
    networks:
      - mvp-network

networks:
  mvp-network:
    name: mvp-network
    external: true
```

### config/broker-master1.conf

```properties
# ============================================================================
# RocketMQ Broker - broker-group1 / Master（broker-master1）
# ============================================================================
#
# 角色：broker-group1 主节点，接受生产写入，并向 Slave1 提供 HA 复制。
#
# 与 docker-compose 对齐（服务名 hostname: broker-master1）：
#   Remoting listenPort = 10911  → compose 映射 10911:10911（宿主机应用直连，建议保留）
#   HA       haListenPort = 10912 → compose 映射 10912:10912（非必须；复制走容器网）
#   VIP      默认 10911-2 = 10909 → compose 映射 10909:10909（非必须；VIP 客户端用）
#
# 主从：Slave1(broker-slave1) 使用 haMasterAddress=broker-master1:10912
#       在 mvp-network 内连接本机 HA，不依赖宿主机端口映射。
#
# 修改本文件后需同步到挂载目录并重启容器（见 deploy/docker/LOCAL-NETWORK.md）：
#   cp deploy/docker/rocketmq-cluster/config/broker-master1.conf \
#      /Users/zhongtao/.my_docker/rocketmq-cluster/config/broker-master1.conf
#
# ============================================================================

# ----------------------------------------------------------------------------
# 一、集群与节点身份
# ----------------------------------------------------------------------------

# 同一集群所有 Broker / NameServer 逻辑上同属一个集群名
brokerClusterName=DefaultCluster

# 主从组名：必须与本组成员（slave1）的 brokerName 相同，且与 group2 不同
brokerName=broker-group1

# 0 = Master；同组内 Slave 为 1、2…
brokerId=0

# ASYNC_MASTER：异步复制，Master 不等待 Slave 确认再返回（吞吐优先，本地开发合适）
# SYNC_MASTER：同步复制，延迟更高、丢消息风险更低
brokerRole=ASYNC_MASTER

# 异步刷盘；与本地磁盘性能匹配，生产可按可靠性改为 SYNC_FLUSH
flushDiskType=ASYNC_FLUSH

# ----------------------------------------------------------------------------
# 二、网络（容器内监听 + 注册给客户端的地址）
# ----------------------------------------------------------------------------

# NameServer：容器网络内用服务名；与 compose 中 namesrv-1/2 一致
namesrvAddr=namesrv-1:9876;namesrv-2:9876

# 注册到 NameServer、再下发给宿主机客户端的 Broker 地址。
# 本地直连模式固定 127.0.0.1，配合 compose 端口映射，避免局域网 IP 变化。
# 注意：容器之间主从复制不要用 brokerIP1，Slave 用 haMasterAddress 容器名。
brokerIP1=127.0.0.1

# Remoting 主端口（生产/消费/管理 RPC）
listenPort=10911

# HA 监听端口：供本 Master 的 Slave 连入做复制（默认常为 listenPort+1，此处显式固定）
haListenPort=10912

# VIP 通道默认端口 = listenPort - 2 = 10909（无需在 conf 写死；compose 已映射便于对照）

# ----------------------------------------------------------------------------
# 三、Topic / 消费组（仅本地开发）
# ----------------------------------------------------------------------------

# 生产环境建议 false，由运维预建 Topic / 订阅组
autoCreateTopicEnable=true
autoCreateSubscriptionGroup=true

# ----------------------------------------------------------------------------
# 四、存储（路径与 compose volume 挂载一致）
# ----------------------------------------------------------------------------

# 每天 04 点删除过期文件
deleteWhen=04
# 消息文件保留小时数（本地开发 48h 即可）
fileReservedTime=48
storePathRootDir=/home/rocketmq/store
storePathCommitLog=/home/rocketmq/store/commitlog
```

### config/broker-slave1.conf

```properties
# ============================================================================
# RocketMQ Broker - broker-group1 / Slave（broker-slave1）
# ============================================================================
#
# 角色：broker-group1 从节点，从 Master1 同步数据；默认不接受生产写入。
#
# 与 docker-compose 对齐（服务名 hostname: broker-slave1）：
#   Remoting listenPort = 10921  → compose 映射 10921:10921（读/客户端，建议保留）
#   HA       haListenPort = 10922 → compose 映射 10922:10922（非必须；本节点不是复制对端）
#   VIP      默认 10921-2 = 10919 → compose 映射 10919:10919（非必须）
#
# 复制方向（与 compose HA 映射无关）：
#   本节点 --Docker 网--> broker-master1:10912（见 haMasterAddress）
#   切勿写成 127.0.0.1:10912（在容器内 127.0.0.1 是自己）
#
# 同步到挂载目录：
#   cp deploy/docker/rocketmq-cluster/config/broker-slave1.conf \
#      /Users/zhongtao/.my_docker/rocketmq-cluster/config/broker-slave1.conf
#
# ============================================================================

# ----------------------------------------------------------------------------
# 一、集群与节点身份
# ----------------------------------------------------------------------------

brokerClusterName=DefaultCluster

# 必须与 Master1 的 brokerName 相同，才能进入同一主从组
brokerName=broker-group1

# 非 0 表示 Slave；同组内多个 Slave 时 brokerId 不可重复
brokerId=1

# 从节点：从 Master 拉/收复制流；不接受 Producer 写入
brokerRole=SLAVE

# 刷盘与 Master 保持一致即可
flushDiskType=ASYNC_FLUSH

# ----------------------------------------------------------------------------
# 二、网络
# ----------------------------------------------------------------------------

namesrvAddr=namesrv-1:9876;namesrv-2:9876

# 下发给宿主机客户端的地址（与 Master 一样用 127.0.0.1 + 端口映射）
brokerIP1=127.0.0.1

# Remoting：与 Master1(10911)、group2 端口错开
listenPort=10921

# 本机 HA 监听（conf 要求配置）；主从场景下由 Slave 去连 Master.HA，本口通常不被对端连入
haListenPort=10922

# 复制目标：Master1 容器名 + Master1 的 haListenPort（必须容器网络可达）
haMasterAddress=broker-master1:10912

# VIP 默认 = 10921 - 2 = 10919（compose 已映射，非业务必需）

# ----------------------------------------------------------------------------
# 三、Topic / 消费组（与 Master 一致，仅本地开发）
# ----------------------------------------------------------------------------

autoCreateTopicEnable=true
autoCreateSubscriptionGroup=true

# ----------------------------------------------------------------------------
# 四、存储
# ----------------------------------------------------------------------------

deleteWhen=04
fileReservedTime=48
storePathRootDir=/home/rocketmq/store
storePathCommitLog=/home/rocketmq/store/commitlog

# ----------------------------------------------------------------------------
# 五、主从同步要点（group1）
# ----------------------------------------------------------------------------
#
# 1. 启动后连接 haMasterAddress（broker-master1:10912）
# 2. 向 Master 报告本地 CommitLog 位点，之后接收增量
# 3. 写入本机 store；模式由 Master 的 brokerRole 决定（当前 ASYNC_MASTER）
# 4. compose 是否映射 10912/10922 到宿主机，不影响容器内复制
#
```

### config/broker-master2.conf

```properties
# ============================================================================
# RocketMQ Broker - broker-group2 / Master（broker-master2）
# ============================================================================
#
# 角色：broker-group2 主节点，与 group1 并行分摊 Topic 队列与流量。
#
# 与 docker-compose 对齐（服务名 hostname: broker-master2）：
#   Remoting listenPort = 10931  → compose 映射 10931:10931（宿主机直连，建议保留）
#   HA       haListenPort = 10932 → compose 映射 10932:10932（非必须；复制走容器网）
#   VIP      默认 10931-2 = 10929 → compose 映射 10929:10929（非必须）
#
# 主从：Slave2 使用 haMasterAddress=broker-master2:10932
#
# 同步到挂载目录：
#   cp deploy/docker/rocketmq-cluster/config/broker-master2.conf \
#      /Users/zhongtao/.my_docker/rocketmq-cluster/config/broker-master2.conf
#
# ============================================================================

# ----------------------------------------------------------------------------
# 一、集群与节点身份
# ----------------------------------------------------------------------------

brokerClusterName=DefaultCluster

# 第二组主从：brokerName 必须与 slave2 相同，且不同于 group1
brokerName=broker-group2

brokerId=0
brokerRole=ASYNC_MASTER
flushDiskType=ASYNC_FLUSH

# ----------------------------------------------------------------------------
# 二、网络
# ----------------------------------------------------------------------------

namesrvAddr=namesrv-1:9876;namesrv-2:9876

# 本地直连：注册 127.0.0.1，由 compose 映射 Remoting 给宿主机应用
brokerIP1=127.0.0.1

# 与 group1（10911/10912）错开，避免端口冲突
listenPort=10931
haListenPort=10932

# VIP 默认 = 10931 - 2 = 10929

# ----------------------------------------------------------------------------
# 三、Topic / 消费组（仅本地开发）
# ----------------------------------------------------------------------------

autoCreateTopicEnable=true
autoCreateSubscriptionGroup=true

# ----------------------------------------------------------------------------
# 四、存储
# ----------------------------------------------------------------------------

deleteWhen=04
fileReservedTime=48
storePathRootDir=/home/rocketmq/store
storePathCommitLog=/home/rocketmq/store/commitlog
```

### config/broker-slave2.conf

```properties
# ============================================================================
# RocketMQ Broker - broker-group2 / Slave（broker-slave2）
# ============================================================================
#
# 角色：broker-group2 从节点，从 Master2 同步数据。
#
# 与 docker-compose 对齐（服务名 hostname: broker-slave2）：
#   Remoting listenPort = 10941  → compose 映射 10941:10941（建议保留）
#   HA       haListenPort = 10942 → compose 映射 10942:10942（非必须）
#   VIP      默认 10941-2 = 10939 → compose 映射 10939:10939（非必须）
#
# 复制：本节点 --Docker 网--> broker-master2:10932
#
# 同步到挂载目录：
#   cp deploy/docker/rocketmq-cluster/config/broker-slave2.conf \
#      /Users/zhongtao/.my_docker/rocketmq-cluster/config/broker-slave2.conf
#
# ============================================================================

# ----------------------------------------------------------------------------
# 一、集群与节点身份
# ----------------------------------------------------------------------------

brokerClusterName=DefaultCluster

# 必须与 Master2 的 brokerName 相同
brokerName=broker-group2

brokerId=1
brokerRole=SLAVE
flushDiskType=ASYNC_FLUSH

# ----------------------------------------------------------------------------
# 二、网络
# ----------------------------------------------------------------------------

namesrvAddr=namesrv-1:9876;namesrv-2:9876
brokerIP1=127.0.0.1

# 与 Master2(10931/10932)、group1 全部端口错开
listenPort=10941
haListenPort=10942

# 复制目标：Master2 容器 HA（容器名，勿用 127.0.0.1）
haMasterAddress=broker-master2:10932

# VIP 默认 = 10941 - 2 = 10939

# ----------------------------------------------------------------------------
# 三、Topic / 消费组（仅本地开发）
# ----------------------------------------------------------------------------

autoCreateTopicEnable=true
autoCreateSubscriptionGroup=true

# ----------------------------------------------------------------------------
# 四、存储
# ----------------------------------------------------------------------------

deleteWhen=04
fileReservedTime=48
storePathRootDir=/home/rocketmq/store
storePathCommitLog=/home/rocketmq/store/commitlog

# ----------------------------------------------------------------------------
# 五、本集群端口总表（与 docker-compose ports 一致）
# ----------------------------------------------------------------------------
#
#   节点              Remoting   HA      VIP(listen-2)   compose 宿主机映射
#   namesrv-1         9876       -       -               9876
#   namesrv-2         9876       -       -               9877→9876
#   broker-master1    10911      10912   10909            全映射；HA/VIP 非业务必需
#   broker-slave1     10921      10922   10919            全映射；HA/VIP 非业务必需
#   broker-master2    10931      10932   10929            全映射；HA/VIP 非业务必需
#   broker-slave2     10941      10942   10939            全映射；HA/VIP 非业务必需
#   dashboard         8082       -       -               8082
#
#   应用：rocketmq.name-server=127.0.0.1:9876;127.0.0.1:9877
#   复制：Slave → Master.HA（容器网）；不依赖 HA 的宿主机映射
#
```
