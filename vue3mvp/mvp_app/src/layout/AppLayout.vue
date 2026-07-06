<script setup lang="ts">
import {computed, onBeforeUnmount, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'

import {getMenuTree, resetMenuTree} from '@/api/apiMenu.ts'
import {useAuthStore} from '@/stores/authStore.ts'
import type {BreadcrumbItem, MenuItem} from '@/types/layoutTypes'
import AppMain from './components/AppMain.vue'
import AppMenu from './components/AppMenu.vue'
import AppTopbar from './components/AppTopbar.vue'

const loading = ref(false)
const logoutLoading = ref(false)

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const menus = ref<MenuItem[]>([])

const currentUser = computed(() => authStore.currentUserInfo)

const breadcrumbs = computed<BreadcrumbItem[]>(() =>
    route.matched
        .filter((matchedRoute) => matchedRoute.meta.title)
        .map((matchedRoute) => ({
          title: matchedRoute.meta.title as string,
          path: matchedRoute.path,
        })),
)

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

const handleLoadMenus = async () => {
  const menuTree = await getMenuTree()
  if (menuTree.length > 0) {
    menus.value = menuTree
    return
  }

  menus.value = await resetMenuTree()
}

onMounted(async () => {
  loading.value = true
  try {
    await authStore.getAuthAction().catch(() => undefined)
    await handleLoadMenus()
  } catch {
    menus.value = []
    ElMessage.error('菜单加载失败')
  } finally {
    loading.value = false
  }
})

window.addEventListener('mvp:menu-updated', handleLoadMenus)

onBeforeUnmount(() => {
  window.removeEventListener('mvp:menu-updated', handleLoadMenus)
})
</script>

<template>
  <!-- 布局 -->
  <el-container class="app-layout">

    <!-- 侧边 -->
    <el-aside class="app-aside" width="200px">
      <AppMenu :menus="menus"/>
    </el-aside>
    <el-container class="app-body">
      <!-- 顶部 -->
      <AppTopbar
          :breadcrumbs="breadcrumbs"
          :loading="loading || logoutLoading"
          :user="currentUser"
          @logout="handleLogout"
      />
      <!-- 主体 -->
      <AppMain/>
    </el-container>

  </el-container>
</template>

<style scoped lang="less">
.app-layout {
  box-sizing: border-box;
  min-height: 100vh;
  color: #1f2937;
  background: #f4f6fa;
}

.app-layout *,
.app-layout *::before,
.app-layout *::after {
  box-sizing: border-box;
}

.app-body {
  min-width: 0;
  flex-direction: column;
}

.app-aside {
  flex-shrink: 0;
  overflow: hidden;
  background: #172033;
}
</style>
