<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import { fetchLayoutData } from '../api/dashboard'
import { useUserStore } from '@/stores/userStore.ts'
import type { BreadcrumbItem, MenuItem } from '../types/types'
import AppMain from './components/LayoutMain.vue'
import AppMenu from './components/LayoutMenu.vue'
import AppTopbar from './components/LayoutTopbar.vue'

// 侧边栏折叠状态统一放在布局组件里管理。
// AppMenu 只负责展示菜单，AppTopbar 只负责触发折叠事件，避免兄弟组件互相依赖。
const isCollapse = ref(false)
const menus = ref<MenuItem[]>([])
const loading = ref(false)
const logoutLoading = ref(false)
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// Element Plus 的 el-aside 通过 width 控制宽度，折叠时保持 64px，展开时保持 220px。
const asideWidth = computed(() => (isCollapse.value ? '64px' : '220px'))

// 顶部栏用户展示统一使用真实 /auth/me 接口返回的数据。
const currentUser = computed(() => userStore.currentUserInfo)

// 根据当前路由 matched 记录生成面包屑，不再依赖 Mock 中写死的 breadcrumbs。
const breadcrumbs = computed<BreadcrumbItem[]>(() =>
  route.matched
    .filter((matchedRoute) => matchedRoute.meta.title)
    .map((matchedRoute) => ({
      title: matchedRoute.meta.title as string,
      path: matchedRoute.path,
    })),
)

// 顶部栏点击折叠按钮时触发，切换左侧菜单展开/收起。
const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value
}

// 个人中心不再放到左侧菜单中，统一从右上角用户下拉菜单进入。
const handleProfile = () => {
  router.push('/home/profile')
}

// 退出登录时先二次确认，再调用 userStore 统一请求登出接口并清理 token。
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
    await userStore.logoutAction()
    ElMessage.success('已退出登录')
  } catch {
    ElMessage.warning('已清理本地登录状态')
  } finally {
    logoutLoading.value = false
    router.replace('/login')
  }
}

// 页面初始化时菜单暂用布局接口；当前用户统一走真实 /auth/me 鉴权接口。
onMounted(async () => {
  loading.value = true
  try {
    const [layoutData] = await Promise.all([
      fetchLayoutData(),
      userStore.loadCurrentAuthAction(),
    ])

    menus.value = layoutData.menus
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <!-- 最外层容器：左侧 el-aside + 右侧主区域。 -->
  <el-container class="app-layout">
    <!-- 左侧栏宽度由 asideWidth 计算属性控制，实现菜单折叠动画。 -->
    <el-aside class="app-aside" :width="asideWidth">
      <AppMenu :collapse="isCollapse" :menus="menus" />
    </el-aside>

    <!-- 右侧区域必须按列布局：顶部 Topbar 在上，主体 Main 在下。 -->
    <el-container class="app-body">
      <AppTopbar
        :breadcrumbs="breadcrumbs"
        :collapse="isCollapse"
        :loading="loading || logoutLoading"
        :user="currentUser"
        @profile="handleProfile"
        @toggle-collapse="toggleSidebar"
        @logout="handleLogout"
      />
      <AppMain />
    </el-container>
  </el-container>
</template>

<style scoped lang="less">
.app-layout {
  /* 当前布局内部统一盒模型，避免修改全局 style.less。 */
  box-sizing: border-box;
  /* 布局容器至少占满整个浏览器高度，侧栏和主体才能拉满全屏。 */
  min-height: 100vh;
  /* 后台主体默认文字颜色放在布局组件内维护。 */
  color: #1f2937;
  /* 后台整体背景色由布局承接，避免依赖全局背景。 */
  background: #f4f6fa;
}

.app-layout *,
.app-layout *::before,
.app-layout *::after {
  /* 后台布局子元素继承 border-box，表格、卡片和工具栏尺寸更稳定。 */
  box-sizing: border-box;
}

.app-body {
  /* 防止右侧内容在表格、长文本场景下把布局横向撑开。 */
  min-width: 0;
  flex-direction: column;
}

.app-aside {
  /* 侧栏不参与压缩，宽度完全交给 el-aside 的 width 属性控制。 */
  flex-shrink: 0;
  /* 折叠菜单时隐藏溢出的文字和菜单内容，避免宽度动画期间露出。 */
  overflow: hidden;
  /* 侧栏主色，和菜单 background-color 保持一致，避免出现色块断层。 */
  background: #172033;
  /* el-aside width 改变时增加过渡，让折叠/展开更自然。 */
  transition: width 0.2s ease;
}
</style>
