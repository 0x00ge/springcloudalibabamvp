<template>
  <el-main class="app-main">
    <section class="content-panel">
      <router-view />
    </section>
  </el-main>
</template>

<style scoped lang="less">
.app-main {
  /* 主内容区高度扣除顶部栏，让页面不足一屏时也能铺满剩余空间。 */
  min-height: calc(100vh - 60px);
  /* 内容区留出页面边距，避免表格和卡片贴边。 */
  padding: 24px;
  /* 右侧主体使用浅灰底，和白色卡片形成层次。 */
  background: #f3f5f9;
}

.content-panel {
  /* 内容容器占满可用宽度，具体页面内部再决定卡片和表格布局。 */
  width: 100%;
  /* 限制超宽屏内容宽度，避免表格横向过分拉伸。 */
  max-width: 1440px;
  /* 最小高度跟随主内容区，保证空页面也有完整背景。 */
  min-height: calc(100vh - 108px);
  /* 居中内容区域，宽屏阅读更稳定。 */
  margin: 0 auto;
}

@media (max-width: 768px) {
  .app-main {
    padding: 14px;
  }
}
</style>
