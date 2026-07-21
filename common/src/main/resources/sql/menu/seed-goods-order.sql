-- =============================================================================
-- 侧栏菜单种子：商品管理 / 订单管理
-- =============================================================================
-- 使用说明：
-- 1. 菜单表 t_menu 按 user_id 隔离，请把下面 @user_id 换成当前登录用户的 32 位 id
-- 2. 也可在前端「菜单管理」里手动新增，路径必须与路由一致：
--      /home/goods
--      /home/order
-- 3. 执行后刷新页面或触发侧栏重新加载即可看到菜单
-- =============================================================================

-- 示例（请替换 user_id）：
-- SET @user_id = '你的用户32位UUIDv7';

-- INSERT INTO t_menu (id, parent_id, title, path, icon, level, sort_order, user_id)
-- VALUES
--   (REPLACE(UUID(), '-', ''), NULL, '商品管理', '/home/goods', NULL, 1, 30, @user_id),
--   (REPLACE(UUID(), '-', ''), NULL, '订单管理', '/home/order', NULL, 1, 40, @user_id);

-- 若已有「系统管理」等父菜单，可把 parent_id 设为父菜单 id，path 仍用 /home/goods、/home/order。
