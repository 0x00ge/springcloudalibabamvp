#!/bin/bash
# ============================================================================
# Replica 初始化脚本：把当前 MySQL 实例挂到对应 master 下
# ============================================================================
#
# 执行时机：
#   - MySQL 官方镜像只会在 /var/lib/mysql 为空、首次初始化数据目录时执行本脚本。
#   - 如果 replica 数据目录已经存在，本脚本不会再次执行。
#   - 修改主从配置后如需重新执行，请先备份或删除对应 replica 的 data 目录。
#
# 运行在哪些容器：
#   - mysql-user-0-replica    -> SOURCE_HOST=mysql-user-0-master
#   - mysql-user-1-replica    -> SOURCE_HOST=mysql-user-1-master
#   - mysql-common-replica    -> SOURCE_HOST=mysql-common-master
#
# 变量来源：
#   - MYSQL_ROOT_PASSWORD、MYSQL_REPL_USER、MYSQL_REPL_PASSWORD、MYSQL_SOURCE_HOST
#     都由 docker-compose 直接写入容器环境变量，不依赖 .env 文件。
#
# 复制方式：
#   - 使用 MySQL 8 的 CHANGE REPLICATION SOURCE TO 语法。
#   - SOURCE_AUTO_POSITION=1 表示使用 GTID 自动定位同步点，不手写 binlog 文件名和 position。
#   - GET_SOURCE_PUBLIC_KEY=1 兼容 caching_sha2_password 认证方式，允许从 master 获取公钥。
#
# 只读保护：
#   - SET PERSIST read_only/super_read_only 会把只读设置持久化到 mysqld-auto.cnf。
#   - SET GLOBAL 立即对当前运行实例生效。
#   - super_read_only 可以阻止拥有 SUPER/高级权限的会话误写从库，更适合开发验证读写分离。
#
# ============================================================================
set -euo pipefail

# 每个 replica 必须知道自己的 master hostname；该 hostname 来自 Docker Compose 的 service DNS。
if [ -z "${MYSQL_SOURCE_HOST:-}" ]; then
  echo "MYSQL_SOURCE_HOST is required for replica initialization" >&2
  exit 1
fi

# 配置 GTID 复制并启动复制线程。
# 注意：这里连接的是当前 replica 容器内的 mysqld，不是 master。
mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='${MYSQL_SOURCE_HOST}',
  SOURCE_PORT=3306,
  SOURCE_USER='${MYSQL_REPL_USER}',
  SOURCE_PASSWORD='${MYSQL_REPL_PASSWORD}',
  SOURCE_AUTO_POSITION=1,
  GET_SOURCE_PUBLIC_KEY=1;
START REPLICA;
SET PERSIST read_only = ON;
SET PERSIST super_read_only = ON;
SET GLOBAL read_only = ON;
SET GLOBAL super_read_only = ON;
SQL
