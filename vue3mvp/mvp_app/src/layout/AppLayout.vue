<script setup lang="ts">
import AppMain from './components/AppMain.vue'
import AppMenu from './components/AppMenu.vue'
import AppTopbar from './components/AppTopbar.vue'
</script>

<template>
  <!-- 布局 -->
  <el-container class="app-layout">

    <!-- 侧边 -->
    <el-aside class="app-aside" width="200px">
      <AppMenu/>
    </el-aside>
    <el-container class="app-body">
      <!-- 顶部 -->
      <AppTopbar/>
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
