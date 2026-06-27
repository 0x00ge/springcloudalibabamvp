#!/bin/bash

echo "================================="
echo "RocketMQ Demo 快速启动脚本"
echo "================================="
echo ""

# 1. 检查 RocketMQ 是否运行
echo "1️⃣ 检查 RocketMQ..."
if docker ps | grep -q rocketmq-namesrv; then
    echo "   ✅ RocketMQ 已运行"
else
    echo "   ⚠️  RocketMQ 未运行，正在启动..."
    cd /Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP/deploy/docker/redis-cluster
    ./init-redis-cluster.sh
    cd /Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP/deploy/docker/rocketmq-cluster
    docker compose -f docker-compose-rocketmq-cluster.yml up -d
    echo "   等待 RocketMQ 启动..."
    sleep 5
fi

echo ""

# 2. 编译项目
echo "2️⃣ 编译项目..."
cd /Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP/rocketmq-demo
mvn clean package -DskipTests -q

if [ $? -eq 0 ]; then
    echo "   ✅ 编译成功"
else
    echo "   ❌ 编译失败"
    exit 1
fi

echo ""

# 3. 启动项目
echo "3️⃣ 启动项目..."
echo "   访问: http://localhost:8080/order/"
echo "   Dashboard: http://localhost:8082"
echo ""
echo "================================="
echo "启动中，请稍候..."
echo "================================="
echo ""

java -jar target/rocketmq-demo-0.0.1-SNAPSHOT.jar
