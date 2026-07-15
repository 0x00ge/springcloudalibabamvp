<script setup lang="ts">
import {computed, onBeforeUnmount, onMounted, ref} from 'vue'
import {useRoute} from 'vue-router'
import {ElMessage} from 'element-plus'

import {getMenuTree, resetMenuTree} from '@/api/apiMenu.ts'
import type {MenuItem} from '@/types/layoutTypes'
import AppMenuItem from '@/layout/components/AppMenuItem.vue'

const route = useRoute()
const loading = ref(false)

// 侧边栏菜单只保存后端返回的数据，不在前端写死兜底菜单。
const menuItems = ref<MenuItem[]>([])

// 生产环境应由 Nginx、gateway 和认证服务保证登录态、转发头、Cookie、Token 校验逻辑一致。
// 前端这里只做一次短重试，用来处理偶发网络抖动；它不是轮询问题的根治方案。
// 重试仍然只请求后端菜单，不改变菜单来源，也不生成前端默认菜单。
const menuRetryDelay = 300

// 菜单加载版本号用于处理并发请求：
// 例如页面刚挂载加载一次菜单，同时菜单管理页又触发 mvp:menu-updated。
// 如果旧请求后返回，不允许它覆盖新请求已经设置好的菜单。
let menuLoadVersion = 0

// 当前激活菜单由侧边栏自己根据路由判断，AppLayout 不再传入菜单状态。
const activeMenu = computed(() => route.path)

// 简单延迟工具，只用于菜单接口失败后的短暂重试。
const sleep = (ms: number) => new Promise((resolve) => window.setTimeout(resolve, ms))

// 从后端加载菜单树：
// 1. 优先读取 /menu/tree，这是用户当前真实菜单。
// 2. 如果后端返回空数组，说明当前用户还没有初始化菜单，再调用 /menu/reset 创建默认菜单。
// 3. 这里不 catch 错误，让外层 reloadMenus 统一处理 loading、重试和错误提示。
const loadMenusFromServer = async () => {
  const menuTree = await getMenuTree()
  if (menuTree.length > 0) {
    return menuTree
  }

  return resetMenuTree()
}

// 菜单加载重试：
// 只在第一次接口调用失败时等待 300ms 后再试一次。
// 重试仍然失败就继续抛给 reloadMenus，最终显示“菜单加载失败”。
const loadMenusWithRetry = async () => {
  try {
    return await loadMenusFromServer()
  } catch {
    await sleep(menuRetryDelay)
    return loadMenusFromServer()
  }
}

// 统一刷新侧边栏菜单。
// 这里是 AppMenu 中唯一负责写入 menuItems 的入口，避免多个地方直接改菜单状态。
const reloadMenus = async () => {
  // 每次开始加载时递增版本号，当前请求只认自己的版本。
  const currentVersion = ++menuLoadVersion
  loading.value = true

  try {
    const nextMenuItems = await loadMenusWithRetry()

    // 只有最后一次发起的加载请求可以更新菜单，防止旧请求晚返回覆盖新数据。
    if (currentVersion === menuLoadVersion) {
      menuItems.value = nextMenuItems
    }
  } catch {
    // 如果当前请求已经不是最新请求，不再清空菜单，也不重复弹错误提示。
    if (currentVersion === menuLoadVersion) {
      menuItems.value = []
      ElMessage.error('菜单加载失败')
    }
  } finally {
    // loading 也只由最后一次请求关闭，避免并发请求导致 loading 提前结束。
    if (currentVersion === menuLoadVersion) {
      loading.value = false
    }
  }
}

// 菜单管理页新增、编辑、删除菜单后，会派发 mvp:menu-updated。
// AppMenu 收到事件后重新请求后端菜单，让侧边栏立刻同步最新树形结构。
const handleMenuUpdated = () => {
  void reloadMenus()
}

onMounted(() => {
  // 侧边栏挂载后加载菜单，AppLayout 不参与菜单获取流程。
  void reloadMenus()

  // 事件监听放在组件挂载后注册，避免模块加载阶段就绑定全局事件。
  window.addEventListener('mvp:menu-updated', handleMenuUpdated)
})

onBeforeUnmount(() => {
  // 组件卸载时让当前未完成的请求失效，避免异步返回后再写已经卸载的菜单状态。
  menuLoadVersion += 1

  // 移除全局事件监听，防止重复进入布局后累积多个监听器。
  window.removeEventListener('mvp:menu-updated', handleMenuUpdated)
})

// 默认展开所有有 children 的父菜单，保证后端返回多级菜单时可以完整显示。
const openedMenus = computed(() => {
  const indexes: string[] = []

  const collectOpenedMenus = (menus: MenuItem[]) => {
    for (const menu of menus) {
      if (menu.children?.length) {
        indexes.push(menu.path || menu.id)
        collectOpenedMenus(menu.children)
      }
    }
  }

  collectOpenedMenus(menuItems.value)

  return indexes
})

const menuRenderKey = computed(() => openedMenus.value.join('|') || 'empty-menu')
</script>

<template>
  <div v-loading="loading" class="menu-shell">
    <div class="brand">
      <span class="brand-title">Vue3 MVP 菜单</span>
    </div>

    <el-menu
        class="side-menu"
        :key="menuRenderKey"
        :default-active="activeMenu"
        :default-openeds="openedMenus"
        background-color="#545c64"
        text-color="#ffffff"
        active-text-color="#0687F1FF"
        router
    >
      <AppMenuItem v-for="item in menuItems" :key="item.id" :menuItem="item"/>
    </el-menu>
  </div>

</template>

<style scoped lang="less">
.menu-shell {
  min-height: 100vh;
}

.brand {
  /* 品牌区使用横向布局，让图标和标题在展开状态下自然排列。 */
  display: flex;
  /* 垂直居中对齐，和顶部栏 60px 高度保持一致。 */
  align-items: center;
  /* 控制图标与标题之间的距离。 */
  gap: 10px;
  /* 固定品牌区高度，和右侧顶部栏对齐。 */
  height: 60px;
  /* 展开状态下给品牌内容留出左右呼吸空间。 */
  padding: 0 20px;
  /* 品牌文字和图标用白色，在深色侧栏上保持对比度。 */
  color: #ffffff;
  /* 底部分割线用于区分品牌区和菜单区，透明白比纯灰更贴合深色背景。 */
  border-bottom: 1px solid rgb(255 255 255 / 8%);
}

.brand-title {
  overflow: hidden;
  /* 品牌名字号略小于页面标题，适合作为侧栏识别信息。 */
  font-size: 17px;
  /* 加粗品牌名，提升左上角项目识别度。 */
  font-weight: 700;
  /* 保证品牌名始终单行显示，折叠时配合 v-show 隐藏。 */
  white-space: nowrap;
}

.side-menu {
  border-right: 0;
  width: 200px;
  background: transparent;
}

.side-menu :deep(.el-menu-item),
.side-menu :deep(.el-sub-menu__title) {
  /* scoped 样式无法直接命中 Element Plus 子组件内部结构，需要使用 :deep。 */
  height: 48px;
  /* line-height 与 height 一致，保证菜单文字垂直居中。 */
  line-height: 48px;
}

</style>
