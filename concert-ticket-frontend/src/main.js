import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

import 'normalize.css/normalize.css'
import '@/styles/index.scss'

import App from './App.vue'
import { createPinia } from 'pinia'
import router from './router'

import installSvgIcon from '@/icons' // 导入修复后的函数
import '@/permission'

// 项目用到的 Element Plus 图标统一登记在 @/icons/element-plus，这里全局注册
import elementPlusIcons from '@/icons/element-plus'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

Object.entries(elementPlusIcons).forEach(([name, icon]) => {
  app.component(name, icon)
})

// 注册 svg 图标组件
installSvgIcon(app)

app.mount('#app')
