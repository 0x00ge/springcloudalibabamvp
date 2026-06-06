# Nacos 2.x Docker 端口映射导致服务注册失败

## 问题现象

`service-order` 启动时，Spring Boot Web 容器已经启动到 `8100` 端口，但随后向 Nacos 注册服务失败，应用退出：

```text
nacos registry, service-order register failed...
NacosException: Client not connected, current status:STARTING
ApplicationContextException: Failed to start bean 'webServerStartStop'
```

同时，Nacos 控制台 `http://127.0.0.1:8848/nacos/` 可以正常访问。

## 根因

Nacos 2.x 不只使用 `8848` 端口。

- `8848`: HTTP 端口，用于控制台和部分 HTTP API。
- `9848`: Nacos 2.x SDK gRPC 端口，Java 客户端注册发现需要连接它。
- `9849`: Nacos 集群 gRPC 端口，单机开发环境建议一并映射，避免后续联调踩坑。

之前的启动命令只映射了 `8848`：

```bash
docker run -d --name my-nacos \
  -p 8848:8848 \
  -e MODE=standalone \
  -v ./my_docker/nacos/logs:/home/nacos/logs \
  -v ./my_docker/nacos/data:/home/nacos/data \
  nacos/nacos-server:v2.5.2
```

这会导致宿主机能访问 Nacos 控制台，但 Spring Cloud Alibaba Nacos Discovery 客户端无法连接 `9848` 完成服务注册，于是报：

```text
Client not connected, current status:STARTING
```

## 修复方式

重建 Nacos 容器，并映射 `9848` 和 `9849`：

```bash
docker stop my-nacos
docker rm my-nacos

docker run -d --name my-nacos \
  -p 8848:8848 \
  -p 9848:9848 \
  -p 9849:9849 \
  -e MODE=standalone \
  -v "$PWD"/my_docker/nacos/logs:/home/nacos/logs \
  -v "$PWD"/my_docker/nacos/data:/home/nacos/data \
  nacos/nacos-server:v2.5.2
```

如果当前目录不是项目根目录，需要把 `"$PWD"/my_docker/...` 换成实际的绝对路径。

## 配置建议

每个服务把服务名、Nacos 地址和 `spring.config.import` 放在基础 `application.yml`，dev/prod 文件只保留端口等环境差异。

`service-order` 示例：

```yaml
spring:
  application:
    name: service-order
  profiles:
    active: dev
  config:
    import: optional:nacos:${spring.application.name}-${spring.profiles.active}.yml
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      discovery:
        server-addr: ${spring.cloud.nacos.server-addr}
      config:
        server-addr: ${spring.cloud.nacos.server-addr}
```

`application-dev.yml` 只保留：

```yaml
server:
  port: 8100
```

这样可以避免 `spring.config.import` 在多个 profile 文件中重复，也能让服务名和 Nacos 地址在配置导入阶段更早可用。

## 验证方式

查看容器端口映射：

```bash
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}"
```

期望能看到类似结果：

```text
0.0.0.0:8848->8848/tcp
0.0.0.0:9848->9848/tcp
0.0.0.0:9849->9849/tcp
```

也可以检查宿主机端口监听：

```bash
lsof -nP -iTCP:8848 -iTCP:9848 -iTCP:9849
```

确认 Nacos 控制台可访问：

```bash
curl -sS -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8848/nacos/
```

返回 `200` 后，重新启动 `service-order`。如果端口映射正确，服务应能成功注册到 Nacos。
