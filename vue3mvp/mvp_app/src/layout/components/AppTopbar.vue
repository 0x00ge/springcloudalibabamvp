<script setup lang="ts">
import { Bell } from '@element-plus/icons-vue'

import type { BreadcrumbItem } from '@/types/layoutTypes'
import type { UserInfo } from '@/types/userTypes'

defineProps<{
  breadcrumbs: BreadcrumbItem[]
  loading: boolean
  user?: UserInfo
}>()

defineEmits<{
  logout: []
}>()
</script>

<template>
  <el-header class="app-header">
    <div class="header-left">
      <el-skeleton v-if="loading" class="breadcrumb-loading" animated :rows="0" />
      <el-breadcrumb v-else separator="/">
        <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.title" :to="item.path">
          {{ item.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="header-right">
      <el-button text circle>
        <el-icon :size="18"><Bell /></el-icon>
      </el-button>
      <el-dropdown>
        <span v-loading="loading" class="user-entry">
          <el-avatar :size="32">{{ user?.name?.slice(0, 1).toUpperCase() || '-' }}</el-avatar>
          <span>{{ user?.name || 'Loading' }}</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="$emit('logout')">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<style scoped lang="less">
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  width: 100%;
  padding: 0 20px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.header-left,
.header-right,
.user-entry {
  display: flex;
  align-items: center;
}

.header-left {
  gap: 12px;
  min-width: 0;
}

.header-right {
  gap: 12px;
}

.breadcrumb-loading {
  width: 120px;
}

.breadcrumb-loading :deep(.el-skeleton__first-line) {
  height: 18px;
  margin-top: 0;
}

.user-entry {
  gap: 8px;
  color: #374151;
  cursor: pointer;
  outline: none;
}

@media (max-width: 768px) {
  .app-header {
    padding: 0 12px;
  }
}
</style>
