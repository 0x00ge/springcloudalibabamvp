<script setup lang="ts">
import type { MenuItem } from '@/types/layoutTypes'

// 单个菜单项数据由父组件 LayoutMenu.vue 传入。
defineProps<{
  menu: MenuItem
}>()
</script>

<template>
  <!-- 有 children 的菜单渲染成二级菜单。 -->
  <el-sub-menu v-if="menu.children?.length" :index="menu.path">
    <template #title>
      <span>{{ menu.title }}</span>
    </template>

    <el-menu-item v-for="child in menu.children" :key="child.id" :index="child.path">
      <template #title>{{ child.title }}</template>
    </el-menu-item>
  </el-sub-menu>

  <!-- 没有 children 的菜单渲染成普通菜单项。 -->
  <el-menu-item v-else :index="menu.path">
    <template #title>{{ menu.title }}</template>
  </el-menu-item>
</template>
