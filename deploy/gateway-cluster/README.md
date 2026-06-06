# 本地 Gateway 双实例集群

本目录用于本地启动 2 个 gateway 实例，并用 Nginx 暴露统一入口。

## 端口规划

| 组件 | 端口 | 说明 |
| --- | --- | --- |
| nginx-gateway | 8000 | 前端和外部请求入口 |
| gateway-1 | 8001 | 第一个 gateway 实例 |
| gateway-2 | 8002 | 第二个 gateway 实例 |

前端 `vite.config.ts` 仍然代理到 `http://127.0.0.1:8000`，不需要改。

## 启动

先确认 Nacos、Redis、用户服务等依赖已经启动，然后执行：

```bash
cd deploy/gateway-cluster
./start-gateway-cluster.sh
```

启动后访问：

```text
http://127.0.0.1:8000
```

Nginx 会把请求轮询转发到：

```text
127.0.0.1:8001
127.0.0.1:8002
```

## 停止

```bash
cd deploy/gateway-cluster
./stop-gateway-cluster.sh
```

## 日志

```text
deploy/gateway-cluster/logs/gateway-8001.log
deploy/gateway-cluster/logs/gateway-8002.log
```

## 注意

`nginx.conf` 里的 upstream 使用 `host.docker.internal`，这是因为 Nginx 运行在 Docker 容器中，而两个 gateway 是宿主机上的 Java 进程。
