#!/bin/bash

echo "================================="
echo "Kafka Demo 快速启动脚本"
echo "================================="
echo ""

PROJECT_DIR="/Users/zhongtao/IdeaProjects/javaProjects/SpringCloudAlibabaMVP"

echo "1. 检查 Kafka..."
if docker ps | grep -q mvp-kafka; then
    echo "   Kafka 容器已运行"
else
    echo "   Kafka 未运行，正在启动..."
    docker network inspect mvp-network >/dev/null 2>&1 || docker network create --subnet 172.30.0.0/16 mvp-network
    cd "$PROJECT_DIR/deploy/docker/kafka-cluster" || exit 1
    docker compose -f docker-compose-kafka-cluster.yml up -d
    echo "   等待 Kafka 启动..."
    sleep 8
fi

echo ""

echo "2. 编译项目..."
cd "$PROJECT_DIR/kafka-demo" || exit 1
mvn clean package -DskipTests -q

if [ $? -eq 0 ]; then
    echo "   编译成功"
else
    echo "   编译失败"
    exit 1
fi

echo ""

echo "3. 启动项目..."
echo "   访问: http://localhost:8091/order/"
echo ""
echo "================================="
echo "启动中，请稍候..."
echo "================================="
echo ""

java -jar target/kafka-demo-0.0.1-SNAPSHOT.jar
