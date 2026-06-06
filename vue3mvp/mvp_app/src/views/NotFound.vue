<script setup lang="ts">
import { useRouter } from 'vue-router'
import { House, RefreshLeft } from '@element-plus/icons-vue'

const router = useRouter()

// 回到后台首页，适合已经登录的用户从错误地址快速回到系统。
const goHome = () => {
  router.push('/home')
}

// 返回上一页，适合用户只是手动输错或点错了链接。
const goBack = () => {
  router.back()
}
</script>

<template>
  <main class="not-found-page">
    <section class="not-found-panel">
      <p class="status-code">404</p>
      <h1>页面不存在</h1>
      <p class="description">当前访问的地址不存在，可能是链接已失效或路径输入有误。</p>

      <div class="actions">
        <el-button type="primary" :icon="House" @click="goHome">返回首页</el-button>
        <el-button :icon="RefreshLeft" @click="goBack">返回上一页</el-button>
      </div>
    </section>
  </main>
</template>

<style scoped lang="less">
.not-found-page {
  /* 404 页面内部统一盒模型，配合全局清零样式保证面板宽度稳定。 */
  box-sizing: border-box;
  /* 404 页面独立展示，不依赖后台 Layout，避免错误地址下出现空布局。 */
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
  background: #f4f6fa;
}

.not-found-page *,
.not-found-page *::before,
.not-found-page *::after {
  /* 只在 404 页面范围内继承 border-box，不修改 style.less。 */
  box-sizing: border-box;
}

.not-found-panel {
  /* 内容区域限制最大宽度，让提示信息在桌面端保持集中易读。 */
  width: min(100%, 520px);
  text-align: center;
}

.status-code {
  /* 404 状态码作为页面主视觉，帮助用户快速识别当前是错误页面。 */
  margin: 0;
  color: #409eff;
  font-size: 96px;
  font-weight: 800;
  line-height: 1;
}

.not-found-panel h1 {
  /* 标题与状态码保持紧凑距离，形成完整的错误提示区域。 */
  margin: 18px 0 10px;
  color: #111827;
  font-size: 28px;
  line-height: 1.3;
}

.description {
  /* 说明文字使用较浅颜色，作为标题的补充信息。 */
  margin: 0;
  color: #64748b;
  font-size: 15px;
  line-height: 1.8;
}

.actions {
  /* 操作按钮集中排列，给用户明确的离开错误页路径。 */
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 28px;
}

@media (max-width: 480px) {
  .status-code {
    /* 小屏降低状态码字号，避免横向溢出。 */
    font-size: 72px;
  }

  .actions {
    /* 小屏按钮纵向排列，更适合窄屏点击。 */
    flex-direction: column;
  }
}
</style>
