import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

import 'normalize.css/normalize.css'
import '@/styles/index.scss'

import App from './App.vue'
import { createPinia } from 'pinia'
import router from './router'

import '@/icons'
import '@/permission'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
