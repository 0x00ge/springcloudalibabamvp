<script setup lang="ts">
import { Bell, Expand, Fold } from '@element-plus/icons-vue'

import type { BreadcrumbItem, UserInfo } from '@/types/types.js'

// collapse 用来决定折叠按钮显示 Fold 还是 Expand 图标。
defineProps<{
  breadcrumbs: BreadcrumbItem[]
  collapse: boolean
  loading: boolean
  user?: UserInfo
}>()

// 顶部栏不直接修改侧栏状态，只向父组件抛出事件，由 AppLayout 统一处理状态变化。
defineEmits<{
  profile: []
  toggleCollapse: []
  logout: []
}>()
</script>

<template>
  <!-- 顶部栏：左侧放折叠按钮和面包屑，右侧放通知和用户入口。 -->
  <el-header class="app-header">
    <div class="header-left">
      <!-- 点击后触发 toggleCollapse 事件，父组件收到后切换侧栏折叠状态。 -->
      <el-button class="collapse-button" text @click="$emit('toggleCollapse')">
        <el-icon :size="20">
          <Fold v-if="!collapse" />
          <Expand v-else />
        </el-icon>
      </el-button>

      <!-- 面包屑由当前路由 matched 动态生成。 -->
      <el-skeleton v-if="loading" class="breadcrumb-loading" animated :rows="0" />
      <el-breadcrumb v-else separator="/">
        <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.title" :to="item.path">
          {{ item.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="header-right">
      <!-- 通知按钮保留入口，后续可接未读消息数量或弹出面板。 -->
      <el-button text circle>
        <el-icon :size="18"><Bell /></el-icon>
      </el-button>
      <!-- 用户下拉菜单：常见后台布局的个人资料和退出登录入口。 -->
      <el-dropdown>
        <span v-loading="loading" class="user-entry">
          <el-avatar :size="32">{{ user?.avatarText || '-' }}</el-avatar>
          <span>{{ user?.name || 'Loading' }}</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="$emit('profile')">个人中心</el-dropdown-item>
            <el-dropdown-item @click="$emit('logout')">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<style scoped lang="less">
.app-header {
  /* 顶部栏使用 flex，让左侧导航信息和右侧操作区分列排布。 */
  display: flex;
  /* 所有顶部栏元素垂直居中，对齐按钮、面包屑、头像。 */
  align-items: center;
  /* 左右两组内容贴两侧排列，中间空间自然留白。 */
  justify-content: space-between;
  /* 顶部栏固定高度，和左侧品牌区高度保持一致。 */
  height: 60px;
  /* 明确占满右侧容器宽度，避免 el-header 在 flex 容器内宽度异常。 */
  width: 100%;
  /* 左右内边距控制顶部栏内容不要贴边。 */
  padding: 0 20px;
  /* 顶部栏使用白底，与浅灰主体区域形成层次。 */
  background: #ffffff;
  /* 底部分割线区分顶部栏和主体内容。 */
  border-bottom: 1px solid #e5e7eb;
}

.header-left,
.header-right,
.user-entry {
  /* 多处横向居中布局共用同一组声明，减少重复样式。 */
  display: flex;
  align-items: center;
}

.header-left {
  /* 折叠按钮和面包屑之间的间距。 */
  gap: 12px;
  /* 防止面包屑过长时把右侧操作区挤出屏幕。 */
  min-width: 0;
}

.header-right {
  /* 通知和用户入口之间保持稳定间距。 */
  gap: 12px;
}

.breadcrumb-loading {
  /* 骨架屏宽度模拟面包屑的大致占位，减少加载完成后的布局跳动。 */
  width: 120px;
}

.breadcrumb-loading :deep(.el-skeleton__first-line) {
  /* 调整 Element Plus 骨架线高度，使其和面包屑文字高度接近。 */
  height: 18px;
  /* 去掉骨架组件默认顶部间距，让它和按钮垂直对齐。 */
  margin-top: 0;
}

.collapse-button {
  /* 折叠按钮固定成正方形，点击热区稳定，不随图标大小变化。 */
  width: 36px;
  height: 36px;
}

.user-entry {
  /* 头像和用户名之间的距离。 */
  gap: 8px;
  /* 用户入口文字颜色比正文略浅，符合顶部工具区层级。 */
  color: #374151;
  /* 鼠标悬停时显示可点击状态。 */
  cursor: pointer;
  /* 去掉 dropdown 触发器聚焦时的浏览器默认描边。 */
  outline: none;
}

@media (max-width: 768px) {
  .app-header {
    /* 小屏收窄顶部栏内边距，给内容留更多空间。 */
    padding: 0 12px;
  }

}
</style>
