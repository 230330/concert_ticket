import router from './router'
import { useUserStore } from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/settings'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import getPageTitle from '@/utils/get-page-title'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register', '/forgot-password']

// 是否已经生成过动态路由
let hasRoles = false

router.beforeEach(async(to, from, next) => {
  NProgress.start()

  document.title = getPageTitle(to.meta.title)

  const hasToken = getToken()

  if (hasToken) {
    if (to.path === '/login' || to.path === '/register' || to.path === '/forgot-password') {
      next({ path: '/' })
      NProgress.done()
    } else {
      const userStore = useUserStore()
      const hasGetUserInfo = userStore.nickname
      if (hasGetUserInfo) {
        next()
      } else {
        try {
          // 获取用户信息（含角色）
          const { roles } = await userStore.getInfoAction()

          // 根据角色生成可访问的路由
          const permissionStore = usePermissionStore()
          const accessRoutes = await permissionStore.generateRoutes(roles || [])

          // 动态添加路由（Vue Router 4 使用 addRoute）
          accessRoutes.forEach(route => {
            router.addRoute(route)
          })

          hasRoles = true
          next({ ...to, replace: true })
        } catch (error) {
          // 移除 token 并跳转到登录页
          hasRoles = false
          await userStore.resetTokenAction()
          ElMessage.error(error || '获取用户信息失败')
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
