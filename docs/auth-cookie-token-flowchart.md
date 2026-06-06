# 认证流程图

```mermaid
sequenceDiagram
    autonumber
    participant JS as 前端 JS
    participant Pinia as Pinia/内存
    participant Cookie as 浏览器 Cookie Jar<br/>HttpOnly
    participant G as gateway
    participant U as service-user-0
    participant R as Redis
    participant B as 业务服务

    rect rgb(238, 247, 255)
        Note over JS,U: 登录：后端把 accessToken 给 JS，把 refreshToken 给浏览器 Cookie Jar
        JS->>G: POST /api/auth/login<br/>body: loginName + password
        G->>U: POST /auth/login
        U->>U: 校验账号、密码、状态
        U->>U: 生成 accessToken + refreshToken
        U->>R: SET auth:refresh:{userId}:{jti}<br/>value=refreshToken, TTL=7天
        U-->>Cookie: Set-Cookie: mvp_refresh_token=refreshToken<br/>HttpOnly; Path=/; SameSite=Lax
        U-->>JS: JSON data.accessToken
        JS->>Pinia: 保存 accessToken + userInfo
        Note over JS,Cookie: JS 不能读取 HttpOnly Cookie，只能读取响应体里的 accessToken
    end

    rect rgb(244, 255, 244)
        Note over JS,B: accessToken 未过期：业务接口只靠 Authorization 鉴权
        JS->>Pinia: 读取 accessToken
        JS->>G: GET /api/**<br/>Authorization: Bearer accessToken
        Cookie-->>G: Cookie 可能被浏览器自动带上<br/>但业务鉴权不使用它
        G->>G: 校验 accessToken 签名、typ=access、exp
        G->>R: GET auth:blacklist:{jti}
        R-->>G: 不存在
        G->>B: 转发请求<br/>X-User-Id / X-Jwt-Id
        B-->>JS: 业务响应
    end

    rect rgb(255, 248, 232)
        Note over JS,U: accessToken 过期或 Pinia/内存丢失：用 Cookie 刷新
        JS->>G: POST /api/auth/refresh<br/>body: 空
        Cookie-->>G: Cookie: mvp_refresh_token=refreshToken
        G->>U: POST /auth/refresh
        U->>U: 从 HttpOnly Cookie 读取 refreshToken
        U->>U: 校验 refreshToken 签名、typ=refresh、exp
        U->>R: GET auth:refresh:{userId}:{jti}
        R-->>U: storedRefreshToken
        U->>U: 对比 Cookie refreshToken 和 Redis value
        U->>R: DEL 旧 refreshToken key
        U->>U: 生成新的 accessToken + refreshToken
        U->>R: SET 新 refreshToken key
        U-->>Cookie: Set-Cookie: mvp_refresh_token=newRefreshToken<br/>覆盖旧 Cookie
        U-->>JS: JSON data.accessToken=newAccessToken
        JS->>Pinia: 覆盖保存新的 accessToken
    end

    rect rgb(255, 239, 239)
        Note over JS,U: 登出：清前端内存、删 Cookie、删 Redis refreshToken、拉黑 accessToken
        JS->>G: POST /api/auth/logout<br/>Authorization: Bearer accessToken
        Cookie-->>G: Cookie: mvp_refresh_token=refreshToken
        G->>G: 校验 accessToken
        G->>U: POST /auth/logout
        U->>U: 再次校验 accessToken
        U->>R: DEL auth:refresh:{userId}:*
        U->>R: SET auth:blacklist:{jti}=1<br/>TTL=accessToken 剩余时间
        U-->>Cookie: Set-Cookie: mvp_refresh_token=;<br/>Max-Age=0
        U-->>JS: 登出成功
        JS->>Pinia: 清空 accessToken + userInfo
    end
```

## HTTP 包关键点

```http
POST /api/auth/login HTTP/1.1
Content-Type: application/json

{"loginName":"13800000000","password":"123456"}
```

```http
HTTP/1.1 200 OK
Set-Cookie: mvp_refresh_token=xxx.yyy.zzz; Path=/; Max-Age=604800; HttpOnly; SameSite=Lax
Content-Type: application/json

{"code":200,"data":{"accessToken":"aaa.bbb.ccc","tokenType":"Bearer","expiresIn":1800,"refreshToken":null,"refreshExpiresIn":604800}}
```

```http
GET /api/** HTTP/1.1
Authorization: Bearer aaa.bbb.ccc
Cookie: mvp_refresh_token=xxx.yyy.zzz
```

```http
POST /api/auth/refresh HTTP/1.1
Cookie: mvp_refresh_token=xxx.yyy.zzz
Content-Length: 0
```
