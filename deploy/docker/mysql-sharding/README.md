# MySQL 主从复制 + User 分库分表 + 公共库

本目录提供一套本地开发用 MySQL 物理拓扑，用来验证：

- **主从复制**：每个物理库都有 1 个 master + 1 个 replica。
- **User 分库分表**：只对用户域做分库分表。
- **公共库**：除 user 以外的表都放在 `mvp_common`。

关键口径：**只有 user 垂直分库并水平分表，其余业务表全部放在公共库中**。

## 拓扑

| 业务域 | 数据库 | 物理表 | 角色 | 容器 | 宿主机端口 |
| --- | --- | --- | --- | --- | --- |
| 用户分片 0 | `mvp_user_0` | `t_user_0`, `t_user_menu_0` | master | `mvp-mysql-user-0-master` | `3310` |
| 用户分片 0 | `mvp_user_0` | `t_user_0`, `t_user_menu_0` | replica | `mvp-mysql-user-0-replica` | `3311` |
| 用户分片 1 | `mvp_user_1` | `t_user_1`, `t_user_menu_1` | master | `mvp-mysql-user-1-master` | `3312` |
| 用户分片 1 | `mvp_user_1` | `t_user_1`, `t_user_menu_1` | replica | `mvp-mysql-user-1-replica` | `3313` |
| 公共库 | `mvp_common` | `t_goods`, `t_order`, `t_order_message_processed` | master | `mvp-mysql-common-master` | `3320` |
| 公共库 | `mvp_common` | `t_goods`, `t_order`, `t_order_message_processed` | replica | `mvp-mysql-common-replica` | `3321` |

数据持久化目录：`/Users/zhongtao/.my_docker/mysql-sharding`。

本配置不使用 `.env` 文件；镜像版本、root 密码、复制账号都直接写在 `docker-compose-mysql-sharding.yml` 中，方便本地开发一眼看清所有配置。

## 启动

先确保项目公共 Docker 网络存在。如果你已经启动过 Redis Cluster，通常已经有这个网络：

```bash
docker network inspect mvp-network >/dev/null 2>&1 || docker network create --subnet 172.19.0.0/16 mvp-network
```

启动 MySQL 拓扑：

```bash
cd /Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP/deploy/docker/mysql-sharding
docker compose -f docker-compose-mysql-sharding.yml up -d
```

停止：

```bash
docker compose -f docker-compose-mysql-sharding.yml down
```

清空后重建：

```bash
docker compose -f docker-compose-mysql-sharding.yml down
mv /Users/zhongtao/.my_docker/mysql-sharding /private/tmp/mysql-sharding-backup-$(date +%Y%m%d%H%M%S)
docker compose -f docker-compose-mysql-sharding.yml up -d
```

## 验证主从复制

以用户分片 0 为例，在 master 写入一条测试数据：

```bash
mysql -h127.0.0.1 -P3310 -uroot -prootroot mvp_user_0 -e "INSERT INTO t_user_0 (id, phone, password_hash, name) VALUES ('018f0000000000000000000000000001', '13900000001', '\$2a\$10\$demo', '复制测试用户');"
```

在 replica 查询：

```bash
mysql -h127.0.0.1 -P3311 -uroot -prootroot mvp_user_0 -e "SELECT id, phone, name FROM t_user_0 WHERE phone='13900000001';"
```

查看复制状态：

```bash
docker exec -it mvp-mysql-user-0-replica mysql -uroot -prootroot -e "SHOW REPLICA STATUS\G"
```

重点看：

| 字段 | 期望值 |
| --- | --- |
| `Replica_IO_Running` | `Yes` |
| `Replica_SQL_Running` | `Yes` |
| `Last_IO_Error` | 空 |
| `Last_SQL_Error` | 空 |

## 分库分表规则建议

Docker Compose 只提供物理库表，不负责 SQL 路由。推荐应用侧使用 ShardingSphere-JDBC 或自定义路由。

推荐逻辑表：

| 逻辑表 | 物理库 | 物理表 |
| --- | --- | --- |
| `t_user` | `mvp_user_0` | `t_user_0` |
| `t_user` | `mvp_user_1` | `t_user_1` |
| `t_user_menu` | `mvp_user_0` | `t_user_menu_0` |
| `t_user_menu` | `mvp_user_1` | `t_user_menu_1` |
| `t_goods` | `mvp_common` | `t_goods` |
| `t_order` | `mvp_common` | `t_order` |
| `t_order_message_processed` | `mvp_common` | `t_order_message_processed` |

推荐用户路由：

```text
CRC32(user_id) % 2 = 0 -> mvp_user_0.t_user_0
CRC32(user_id) % 2 = 1 -> mvp_user_1.t_user_1

CRC32(user_id) % 2 = 0 -> mvp_user_0.t_user_menu_0
CRC32(user_id) % 2 = 1 -> mvp_user_1.t_user_menu_1
```

如果注册或登录时只有手机号，也可以先按 `CRC32(phone) % 2` 路由到用户分片；但菜单表必须跟随用户 ID 或用户所属分片，保证同一用户的数据落在同一个用户分片库。

## Spring Boot 数据源示例

当前项目使用 `dynamic-datasource`，可以先按物理库配置多个数据源；用户逻辑表路由建议后续接 ShardingSphere-JDBC 或在用户服务内封装路由。

```yaml
spring:
  datasource:
    dynamic:
      primary: user0Master
      strict: true
      datasource:
        user0Master:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://127.0.0.1:3310/mvp_user_0?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
          username: root
          password: rootroot
        user0Replica:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://127.0.0.1:3311/mvp_user_0?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
          username: root
          password: rootroot
        user1Master:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://127.0.0.1:3312/mvp_user_1?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
          username: root
          password: rootroot
        user1Replica:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://127.0.0.1:3313/mvp_user_1?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
          username: root
          password: rootroot
        commonMaster:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://127.0.0.1:3320/mvp_common?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
          username: root
          password: rootroot
        commonReplica:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://127.0.0.1:3321/mvp_common?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
          username: root
          password: rootroot
```

## 注意事项

- 本配置是本地开发环境，密码固定为 `rootroot` / `replpass`，不要直接用于生产。
- 每个 replica 首次初始化时自动执行 `CHANGE REPLICATION SOURCE TO ... SOURCE_AUTO_POSITION=1`。
- 主从复制使用 GTID，master 和 replica 都开启 `binlog-format=ROW`。
- replica 初始化后会开启 `read_only` 和 `super_read_only`，避免误写从库。
- 如果修改 init SQL，已有数据卷不会自动重新执行；需要备份或删除 `/Users/zhongtao/.my_docker/mysql-sharding` 后重建。
- 当前 compose 不包含 ShardingSphere-Proxy；本项目 Java 服务跑在宿主机，优先建议用应用侧 ShardingSphere-JDBC 或服务内路由接入。
