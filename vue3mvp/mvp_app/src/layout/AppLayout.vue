<script setup lang="ts">
/**
 * @file Layout.vue
 * @description 后台管理系统的根布局组件，整合侧边栏、顶部导航和主体内容区域。
 *              该组件负责管理侧边栏折叠状态、菜单数据、用户信息、面包屑导航，
 *              并提供退出登录、个人中心跳转等全局交互。
 *              所有子组件（AppMenu、AppTopbar、AppMain）通过 props 和 events 通信，
 *              避免兄弟组件直接依赖，提升可维护性。
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import { fetchLayoutData } from '../api/dashboard' // 获取菜单等布局数据的 API
import { useAuthStore } from '@/stores/authStore.ts' // 认证状态管理（当前登录用户、登录状态、退出操作）
import type { BreadcrumbItem, MenuItem } from '../types/types' // 类型定义
import AppMain from './components/LayoutMain.vue' // 主体内容区域（路由出口）
import AppMenu from './components/LayoutMenu.vue' // 左侧菜单组件
import AppTopbar from './components/LayoutTopbar.vue' // 顶部导航栏组件

// ==================== 响应式状态 ====================

/** 侧边栏折叠状态，统一由本组件管理，通过 props 传递给 AppMenu，通过事件监听来自 AppTopbar 的切换请求。 */
const isCollapse = ref(false)

/** 菜单数据（从后端或静态配置获取），由 onMounted 中调用 fetchLayoutData 填充。 */
const menus = ref<MenuItem[]>([])

/** 布局数据加载状态，用于顶部栏展示加载指示（如退出按钮 loading）。 */
const loading = ref(false)

/** 退出登录按钮的加载状态，防止重复点击。 */
const logoutLoading = ref(false)

// Vue Router 实例，用于获取当前路由信息和执行路由跳转。
const route = useRoute()
const router = useRouter()

// 认证 store，用于获取当前登录用户信息和执行退出登录 action。
const authStore = useAuthStore()

// ==================== 计算属性 ====================

/**
 * 侧边栏宽度，根据折叠状态动态计算。
 * Element Plus 的 el-aside 通过 width 属性控制宽度，
 * 折叠时保持 64px（仅显示图标），展开时为 220px（显示图标+文字）。
 * 该值绑定到 el-aside 的 :width 属性，并配合 CSS transition 实现平滑动画。
 */
const asideWidth = computed(() => (isCollapse.value ? '64px' : '220px'))

/**
 * 当前登录用户信息，直接从 authStore 中获取。
 * 确保顶部栏展示的用户名、头像等来源于真实鉴权接口（/auth/me），而非 Mock 数据。
 */
const currentUser = computed(() => authStore.currentUserInfo)

/**
 * 面包屑导航数据，根据当前路由的 matched 记录动态生成。
 * 过滤出 meta.title 存在的路由记录，并映射为 { title, path } 格式。
 * 这样不再依赖后端返回的 breadcrumbs，更加灵活和准确。
 */
const breadcrumbs = computed<BreadcrumbItem[]>(() =>
  route.matched
    .filter((matchedRoute) => matchedRoute.meta.title)
    .map((matchedRoute) => ({
      title: matchedRoute.meta.title as string,
      path: matchedRoute.path,
    })),
)

// ==================== 事件处理函数 ====================

/**
 * 切换侧边栏折叠/展开状态。
 * 由顶部栏的菜单按钮触发，通过 @toggle-collapse 事件传递到此。
 */
const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value
}

/**
 * 跳转到个人中心页面。
 * 由顶部栏用户下拉菜单中的“个人中心”项触发。
 * 路由路径为 '/home/profile'，需确保该路由已注册。
 */
const handleProfile = () => {
  router.push('/home/profile')
}

/**
 * 处理退出登录流程。
 * 1. 弹出二次确认框，防止误操作。
 * 2. 若确认，设置 logoutLoading 为 true 禁用按钮。
 * 3. 调用 authStore.logoutAction() 执行退出接口请求并清理 token。
 * 4. 成功后提示并跳转至登录页；失败时给出友好提示。
 * 5. 无论成功失败，最终重置 loading 状态。
 */
const handleLogout = async () => {
  // 二次确认
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
    // 即使接口报错，store 内部可能已清理本地 token，提示用户但继续跳转。
    ElMessage.warning('已清理本地登录状态')
  } finally {
    logoutLoading.value = false
    router.replace('/login')
  }
}

// ==================== 生命周期 ====================

/**
 * 组件挂载完成后执行初始化。
 * 1. 并行请求布局数据（菜单等）和登录用户信息（通过 authStore.loadCurrentAuthAction 调用 /auth/me）。
 *    使用 Promise.all 提高加载效率。
 * 2. 将获取到的菜单数据赋值给 menus。
 * 3. 无论成功或失败，最终关闭 loading 状态。
 */
onMounted(async () => {
  loading.value = true
  try {
    const [layoutData] = await Promise.all([
      fetchLayoutData(),
      authStore.loadCurrentAuthAction(), // 该 action 会请求 /auth/me 并更新 authStore.currentUserInfo
    ])

    menus.value = layoutData.menus
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <!--
    ==================== 模板结构 ====================
    最外层 el-container 实现经典后台布局：左侧侧边栏 + 右侧（顶部栏 + 主体）。
  -->
  <el-container class="app-layout">
    <!--
      左侧侧边栏（el-aside）
      - width 动态绑定 asideWidth，实现折叠/展开动画。
      - flex-shrink: 0 防止被压缩，确保宽度固定。
      - 内部包含 AppMenu 组件，传递折叠状态和菜单数据。
    -->
    <el-aside class="app-aside" :width="asideWidth">
      <AppMenu :collapse="isCollapse" :menus="menus" />
    </el-aside>

    <!--
      右侧主体容器（el-container）
      - 采用 flex-direction: column 布局，使顶部栏和主体垂直排列。
      - min-width: 0 防止内容撑开容器。
    -->
    <el-container class="app-body">
      <!--
        顶部导航栏（AppTopbar）
        - 接收面包屑、折叠状态、加载状态、用户信息等 props。
        - 通过事件监听：toggle-collapse（折叠切换）、profile（个人中心）、logout（退出登录）。
      -->
      <AppTopbar
        :breadcrumbs="breadcrumbs"
        :collapse="isCollapse"
        :loading="loading || logoutLoading"
        :user="currentUser"
        @profile="handleProfile"
        @toggle-collapse="toggleSidebar"
        @logout="handleLogout"
      />

      <!--
        主体内容区域（AppMain）
        - 内部通常包含 <router-view>，用于渲染当前路由对应的页面组件。
      -->
      <AppMain />
    </el-container>
  </el-container>
</template>

<style scoped lang="less">
/**
 * ==================== 样式 ====================
 * 以下样式仅作用于本组件，使用 scoped 避免污染全局。
 * 布局颜色、尺寸、过渡效果在此定义，保持整体风格一致。
 */

.app-layout {
  /* 统一盒模型，避免全局影响 */
  box-sizing: border-box;
  /* 至少占满视口高度，保证侧边栏和背景能拉满全屏 */
  min-height: 100vh;
  /* 默认文字颜色，与背景搭配 */
  color: #1f2937;
  /* 全局背景色，避免依赖全局样式，使得组件更独立 */
  background: #f4f6fa;
}

.app-layout *,
.app-layout *::before,
.app-layout *::after {
  /* 所有子元素继承 border-box，确保尺寸计算一致，尤其对表格、卡片等组件友好 */
  box-sizing: border-box;
}

.app-body {
  /* 防止右侧内容（如长表格）横向撑开布局，保持自适应 */
  min-width: 0;
  /* 垂直排列顶部栏和主体 */
  flex-direction: column;
}

.app-aside {
  /* 侧边栏不参与 flex 压缩，宽度完全由 el-aside 的 width 属性控制 */
  flex-shrink: 0;
  /* 折叠时隐藏溢出内容（文字），避免宽度动画期间残留文字 */
  overflow: hidden;
  /* 侧边栏背景色，与菜单背景一致，避免色差 */
  background: #172033;
  /* 宽度变化时添加过渡动画，使折叠/展开更平滑 */
  transition: width 0.2s ease;
}
</style>
