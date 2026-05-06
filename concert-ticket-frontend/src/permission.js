import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import getPageTitle from '@/utils/get-page-title'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

// 是否已经生成过动态路由
let hasRoles = false

router.beforeEach(async(to, from, next) => {
  NProgress.start()

  document.title = getPageTitle(to.meta.title)

  const hasToken = getToken()

  if (hasToken) {
    if (to.path === '/login' || to.path === '/register') {
      next({ path: '/' })
      NProgress.done()
    } else {
      const hasGetUserInfo = store.getters.nickname
      if (hasGetUserInfo) {
        next()
      } else {
        try {
          // 获取用户信息（含角色）
          const { roles } = await store.dispatch('user/getInfo')

          // 根据角色生成可访问的路由
          const accessRoutes = await store.dispatch('permission/generateRoutes', roles || [])

          // 动态添加路由
          router.addRoutes(accessRoutes)

          // hack 方法，确保 addRoutes 已完成
          // replace: true 使导航不留下历史记录
          hasRoles = true
          next({ ...to, replace: true })
        } catch (error) {
          // 移除 token 并跳转到登录页
          hasRoles = false
          await store.dispatch('user/resetToken')
          Message.error(error || '获取用户信息失败')
          next(`/login?redirect=${to.path}`)
          NProgress.done()
        }
      }
    }
  } else {
    hasRoles = false
    if (whiteList.indexOf(to.path) !== -1) {
      next()
    } else {
      next(`/login?redirect=${to.path}`)
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
