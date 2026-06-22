#!/usr/bin/env bash
# Redis Cluster 一键启动和初始化脚本。
#
# 做的事情：
#   1. 创建 6 个节点的数据目录；
#   2. docker compose 启动 6 个 Redis 容器；
#   3. 等待每个节点 redis-cli ping 成功；
#   4. 如果集群已经是 ok 状态，直接打印节点信息并退出；
#   5. 如果还没初始化，执行 redis-cli --cluster create 创建 3 主 3 从。
#
# 注意：
#   - 本脚本不会自动删除旧数据。要重建集群，请先按 README 执行 rm -rf 数据目录。
#   - cluster create 只能在空节点上执行；节点已有 nodes.conf 或旧数据时需要先清理。
set -euo pipefail

# Compose 文件名。脚本固定在当前目录执行，避免从其他目录调用时找不到文件。
COMPOSE_FILE="docker-compose-redis-cluster.yml"

# Redis 数据持久化目录。每个节点都要单独目录，否则 nodes.conf 和 AOF 会互相覆盖。
DATA_DIR="/Users/zhongtao/.my_docker/redis-cluster"

# 切到脚本所在目录，保证 docker compose -f 使用相对路径也稳定。
cd "$(dirname "$0")"

# 提前创建目录，避免 Docker 自动用 root 权限创建后本机清理不方便。
mkdir -p \
  "${DATA_DIR}/7001/data" \
  "${DATA_DIR}/7002/data" \
  "${DATA_DIR}/7003/data" \
  "${DATA_DIR}/7004/data" \
  "${DATA_DIR}/7005/data" \
  "${DATA_DIR}/7006/data"

# 启动 6 个 Redis 节点。此时只是 cluster-enabled 的 Redis 节点，还没有分配槽位。
docker compose -f "${COMPOSE_FILE}" up -d

echo "Waiting Redis nodes to be ready..."
for port in 7001 7002 7003 7004 7005 7006; do
  # Redis 容器启动后还需要一点时间接受连接。
  # ping 成功说明 redis-server 已经开始监听当前节点端口。
  until docker exec "mvp-redis-${port}" redis-cli -p "${port}" ping >/dev/null 2>&1; do
    sleep 1
  done
done

# 如果集群已经初始化过，重复执行脚本时不要再次 cluster create。
# 这样脚本可以安全重复运行，用来启动已存在的集群。
if docker exec mvp-redis-7001 redis-cli -p 7001 cluster info 2>/dev/null | grep -q "cluster_state:ok"; then
  echo "Redis Cluster already initialized."
  docker exec mvp-redis-7001 redis-cli -p 7001 cluster nodes
  exit 0
fi

echo "Creating Redis Cluster: 3 masters + 3 replicas..."
# --cluster-replicas 1 表示每个 master 配 1 个 replica。
# 传入 6 个节点时，redis-cli 会创建 3 个 master，并把剩余 3 个节点分配为 replica。
# 节点地址必须使用宿主机可访问地址，和 compose 中 cluster-announce-hostname/port 保持一致。
docker exec -i mvp-redis-7001 redis-cli --cluster create \
  host.docker.internal:7001 \
  host.docker.internal:7002 \
  host.docker.internal:7003 \
  host.docker.internal:7004 \
  host.docker.internal:7005 \
  host.docker.internal:7006 \
  --cluster-replicas 1 \
  --cluster-yes

echo "Redis Cluster initialized."
# 打印节点、槽位和主从关系，方便确认初始化结果。
docker exec mvp-redis-7001 redis-cli -p 7001 cluster nodes
