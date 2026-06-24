<script setup lang="ts">
import { Menu, OfficeBuilding, Setting, Tickets, User, UserFilled } from '@element-plus/icons-vue'

import type { Component } from 'vue'
import type { MenuIconName, MenuItem } from '@/types/types.js'

// 菜单图标映射表：后端/mock 只返回图标名称，组件里再映射成真正的 Element Plus 图标组件。
const iconMap: Record<MenuIconName, Component> = {
  Menu,
  OfficeBuilding,
  Setting,
  Tickets,
  User,
  UserFilled,
}

// 单个菜单项数据由父组件 LayoutMenu.vue 传入。
defineProps<{
  menu: MenuItem
}>()
</script>

<template>
  <!-- 有 children 的菜单渲染成二级菜单。 -->
  <el-sub-menu v-if="menu.children?.length" :index="menu.path">
    <template #title>
      <el-icon v-if="menu.icon"><component :is="iconMap[menu.icon]" /></el-icon>
      <span>{{ menu.title }}</span>
    </template>

    <el-menu-item v-for="child in menu.children" :key="child.id" :index="child.path">
      <el-icon v-if="child.icon"><component :is="iconMap[child.icon]" /></el-icon>
      <template #title>{{ child.title }}</template>
    </el-menu-item>
  </el-sub-menu>

  <!-- 没有 children 的菜单渲染成普通菜单项。 -->
  <el-menu-item v-else :index="menu.path">
    <el-icon v-if="menu.icon"><component :is="iconMap[menu.icon]" /></el-icon>
    <template #title>{{ menu.title }}</template>
  </el-menu-item>
</template>
