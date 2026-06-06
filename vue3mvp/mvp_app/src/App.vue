<template>
  <router-view />
</template>

<style lang="less">
html,
body,
#app {
  /* 根节点高度放在 App 层，配合 style.less 的清零样式让页面能撑满视口。 */
  min-height: 100vh;
}

body {
  /* 项目默认字体放在应用入口，避免污染 style.less 中你保留的清零规则。 */
  color: #1f2937;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', Arial, sans-serif;
  line-height: 1.5;
  background: #f4f6fa;
}

a {
  /* 链接继承父级颜色，避免浏览器默认蓝色影响后台系统样式。 */
  color: inherit;
  text-decoration: none;
}

button,
input,
textarea,
select {
  /* 原生表单元素继承字体，让它们和 Element Plus 组件文字保持一致。 */
  font: inherit;
}
</style>
