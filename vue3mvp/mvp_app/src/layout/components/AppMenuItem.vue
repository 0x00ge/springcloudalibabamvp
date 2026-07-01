<script setup lang="ts">
import type { MenuItem } from '@/types/layoutTypes'

defineOptions({
  name: 'AppMenuItem',
})

// 单个菜单项数据由父组件 AppMenu.vue 传入。
defineProps<{
  menu: MenuItem
}>()
</script>

<template>
  <!-- 有 children 的菜单继续递归渲染，支持任意层级子菜单。 -->
  <el-sub-menu v-if="menu.children?.length" :index="menu.path || menu.id">
    <template #title>
      <span>{{ menu.title }}</span>
    </template>

    <AppMenuItem v-for="child in menu.children" :key="child.id" :menu="child" />
  </el-sub-menu>

  <!-- 没有 children 的菜单渲染成普通菜单项。 -->
  <el-menu-item v-else :index="menu.path">
    <template #title>{{ menu.title }}</template>
  </el-menu-item>
</template>
