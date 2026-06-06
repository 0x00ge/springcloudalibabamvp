import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 使用你定义的全局清零样式，其他视觉和布局样式放在具体组件中维护。
import './style.less'

import App from './App.vue'
import { setupMock } from './mock'
import router from './router'

setupMock()

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
