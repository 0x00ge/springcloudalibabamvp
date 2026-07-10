<script setup lang="ts">
import {computed, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'

import type {BreadcrumbItem} from '@/types/layoutTypes'
import {useAuthStore} from '@/stores/authStore.ts'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const logoutLoading = ref(false)

// 顶部栏自己根据当前路由生成面包屑，AppLayout 不再关心路由展示细节。
const breadcrumbs = computed<BreadcrumbItem[]>(() =>
    route.matched
        .filter((matchedRoute) => matchedRoute.meta.title)
        .map((matchedRoute) => ({
          title: matchedRoute.meta.title as string,
          path: matchedRoute.path,
        })),
)

// 顶部栏自己从 authStore 读取用户展示信息，AppLayout 只负责放置顶部栏。
const currentUser = computed(() => authStore.currentUserInfo)

// 退出登录属于顶部用户下拉菜单的行为，直接放在 AppTopbar 内部维护。
const handleLogout = async () => {
  const confirmed = await ElMessageBox.confirm('确定退出当前账号吗？', '退出登录', {
    type: 'warning',
    confirmButtonText: '退出',
    cancelButtonText: '取消',
  })
      .then(() => true)
      .catch(() => false)

  if (!confirmed) return

  logoutLoading.value = true

  try {
    await authStore.logoutAction()
    ElMessage.success('已退出登录')
  } catch {
    ElMessage.warning('已清理本地登录状态')
  } finally {
    logoutLoading.value = false
    router.replace('/login')
  }
}
</script>

<template>
  <el-header class="app-header">

    <div class="header-left">
      <!-- 面包屑 -->
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.title" :to="item.path">
          {{ item.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="header-right">
      <!-- 下拉菜单 -->
      <el-dropdown :disabled="logoutLoading">
        <div class="user-entry">
          <el-avatar :size="32">
            {{ currentUser.name?.slice(0, 1).toUpperCase() || '-' }}
          </el-avatar>
          <div>{{ currentUser.name || 'Loading' }}</div>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
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
