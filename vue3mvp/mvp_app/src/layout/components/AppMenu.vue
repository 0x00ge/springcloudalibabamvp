<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { MenuItem } from '@/types/layoutTypes'
import LayoutMenuItem from '@/layout/components/AppMenuItem.vue'

const route = useRoute()
const activeMenu = computed(() => route.path)

// 子组件接收父组件数据
defineProps<{
  menus: MenuItem[]
}>()
</script>

<template>
  <!-- 品牌区。 -->
  <div class="brand">
    <span class="brand-title">Vue3 MVP</span>
  </div>

  <!--
    Element Plus 菜单：
    - router 表示点击菜单项时使用 vue-router 跳转
    - index 建议与路由 path 保持一致，后续接真实页面时更容易维护
  -->
  <el-menu
    class="side-menu"
    :default-active="activeMenu"
    background-color="#172033"
    text-color="#c8d1e2"
    active-text-color="#ffffff"
    router
  >
    <!-- 菜单项拆到独立组件中，父组件只负责循环数据，子组件负责判断普通菜单/二级菜单。 -->
    <LayoutMenuItem v-for="menu in menus" :key="menu.id" :menu="menu" />
  </el-menu>
</template>

<style scoped lang="less">
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
  /* 去掉 Element Plus 菜单默认右边框，侧栏整体更干净。 */
  border-right: 0;
  width: 220px;
}

.side-menu :deep(.el-menu-item),
.side-menu :deep(.el-sub-menu__title) {
  /* scoped 样式无法直接命中 Element Plus 子组件内部结构，需要使用 :deep。 */
  height: 48px;
  /* line-height 与 height 一致，保证菜单文字垂直居中。 */
  line-height: 48px;
}

</style>
