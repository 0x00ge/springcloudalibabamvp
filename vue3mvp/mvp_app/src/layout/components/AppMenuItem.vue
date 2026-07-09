<script setup lang="ts">
import type { MenuItem } from '@/types/layoutTypes'

/**
 * 定义组件选项
 * - name: 组件名称，用于递归调用自身和 Vue DevTools 调试
 */
defineOptions({
  name: 'AppMenuItem',
})

/**
 * 组件 Props
 * 接收一个菜单项数据，由父组件 AppMenu.vue 传入
 */
const props = defineProps<{
  menuItem: MenuItem
}>()
</script>

<template>
  <!--
    有子菜单的情况：渲染为可展开的子菜单
    判断条件：menu.children 存在且有内容
  -->
  <el-sub-menu
      v-if="props.menuItem.children?.length"
      :index="props.menuItem.path || props.menuItem.id"
  >
    <!-- 子菜单标题插槽 -->
    <template #title>
      <span>{{ props.menuItem.title }}</span>
    </template>

    <!--
      递归渲染：遍历所有子菜单项
      每个子项继续使用 AppMenuItem 组件，支持任意层级嵌套
    -->
    <AppMenuItem
      v-for="item in props.menuItem.children"
      :key="item.id"
      :menuItem="item"
    />
  </el-sub-menu>

  <!--
    没有子菜单的情况：渲染为普通菜单项（叶子节点）
    点击后跳转到对应的 path 路径
  -->
  <el-menu-item v-else :index="props.menuItem.path">
    <!-- 菜单项标题 -->
    <template #title>{{ props.menuItem.title }}</template>
  </el-menu-item>
</template>