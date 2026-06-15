# SQL 文件目录说明

## 📁 目录结构

```
sql/
├── init_all.sql           # 一键初始化所有表（推荐）
├── user/
│   └── tables.sql         # 用户服务表（t_user）
├── goods/
│   └── tables.sql         # 商品服务表（t_goods）
└── order/
    └── tables.sql         # 订单服务表（t_order, t_order_message_processed）
```

## 🚀 使用方式

### 方式 1：一键执行所有表（推荐）

```bash
# 进入 SQL 目录
cd common/src/main/resources/sql

# 一次性创建所有表
mysql -u root -p mvp < init_all.sql
```

### 方式 2：按服务分别执行

```bash
# 用户服务
mysql -u root -p mvp < user/tables.sql

# 商品服务
mysql -u root -p mvp < goods/tables.sql

# 订单服务
mysql -u root -p mvp < order/tables.sql
```

### 方式 3：在 MySQL 客户端内执行

```sql
USE mvp;

SOURCE user/tables.sql;
SOURCE goods/tables.sql;
SOURCE order/tables.sql;
```

## 📋 表清单

| 服务 | 表名 | 说明 |
|-----|------|------|
| **service-user-0** | `t_user` | 用户表（手机号、邮箱、密码、基本信息） |
| **service-goods-0** | `t_goods` | 商品表（商品信息、库存、限购、时间窗口） |
| **service-order-0** | `t_order` | 订单表（订单信息、金额、状态） |
| **service-order-0** | `t_order_message_processed` | 消息去重表（RocketMQ 幂等性） |

## 🔧 注意事项

1. **执行前确保数据库已创建**：
   ```sql
   CREATE DATABASE IF NOT EXISTS mvp DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **脚本包含 IF NOT EXISTS**，可重复执行，不会报错

3. **按服务拆分的好处**：
   - 微服务架构下，各服务可独立管理自己的表
   - 便于版本控制和迁移管理
   - 清晰的服务边界

4. **旧文件已废弃**：
   - ~~mvp.sql~~（已拆分为 user/goods/order）
   - ~~order_message_processed.sql~~（已合并到 order/tables.sql）

## 📊 表关系

```
t_user (用户表)
    ↓ user_id
t_order (订单表)
    ↓ goods_id
t_goods (商品表)
```

- `t_order.user_id` → `t_user.id`（外键关系，未强制约束）
- `t_order.goods_id` → `t_goods.id`（外键关系，未强制约束）
- `t_order_message_processed` 独立表，用于 RocketMQ 消息去重

## 🎯 最佳实践

- **开发环境**：使用 `init_all.sql` 一键初始化
- **生产环境**：使用 Flyway 或 Liquibase 管理数据库版本
- **测试数据**：可在各服务目录下添加 `test_data.sql`

## 🔄 升级说明

从旧版本迁移：

```bash
# 1. 备份现有数据
mysqldump -u root -p mvp > backup_$(date +%Y%m%d).sql

# 2. 删除旧表（可选，如果需要重建）
# DROP TABLE IF EXISTS t_user, t_goods, t_order;

# 3. 执行新版本脚本
mysql -u root -p mvp < init_all.sql
```

---

有问题请查看项目文档：`docs/architecture-design.md`
