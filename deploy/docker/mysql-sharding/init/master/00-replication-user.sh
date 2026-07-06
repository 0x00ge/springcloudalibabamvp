#!/bin/bash
# ============================================================================
# Master 初始化脚本：创建 MySQL 主从复制账号
# ============================================================================
#
# 执行时机：
#   - MySQL 官方镜像只会在 /var/lib/mysql 为空、首次初始化数据目录时执行
#     /docker-entrypoint-initdb.d 下的 .sh / .sql 文件。
#   - 如果数据卷已经存在，本脚本不会再次执行；需要重建数据请先备份或删除数据目录。
#
# 运行在哪些容器：
#   - mysql-user-0-master
#   - mysql-user-1-master
#   - mysql-common-master
#
# 变量来源：
#   - MYSQL_ROOT_PASSWORD、MYSQL_REPL_USER、MYSQL_REPL_PASSWORD 都由 docker-compose
#     直接写入容器环境变量，不依赖 .env 文件。
#
# 为什么创建 REPLICATION CLIENT：
#   - REPLICATION SLAVE/REPLICA 用于允许从库拉取 binlog。
#   - REPLICATION CLIENT 用于查看复制状态、binlog 状态等，方便本地排查。
#
# ============================================================================
set -euo pipefail

# 使用 root 账号连接当前 master，并创建复制账号。
# CREATE USER IF NOT EXISTS 让脚本具备幂等性；虽然官方镜像首次初始化才执行，幂等写法更安全。
mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE USER IF NOT EXISTS '${MYSQL_REPL_USER}'@'%' IDENTIFIED BY '${MYSQL_REPL_PASSWORD}';
GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO '${MYSQL_REPL_USER}'@'%';
FLUSH PRIVILEGES;
SQL
