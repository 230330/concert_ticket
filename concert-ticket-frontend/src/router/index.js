import { createRouter, createWebHistory } from 'vue-router'

/* Layout */
import Layout from '@/layout/index.vue'

/**
 * constantRoutes
 * 无需权限的基础路由，所有用户都能看到
 */
export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    hidden: true
  },

  {
    path: '/register',
    component: () => import('@/views/register/index.vue'),
    hidden: true
  },

  {
    path: '/forgot-password',
    component: () => import('@/views/forgot-password/index.vue'),
    hidden: true
  },

  {
    path: '/404',
    component: () => import('@/views/404.vue'),
    hidden: true
  },

  // ===================== 前台模块 =====================
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [{
      path: 'dashboard',
      name: 'Dashboard',
      component: () => import('@/views/dashboard/index.vue'),
      meta: { title: '首页', icon: 'dashboard' }
    }]
  },

  {
    path: '/concert',
    component: Layout,
    redirect: '/concert/list',
    name: 'Concert',
    meta: { title: '演唱会', icon: 'Headset' },
    children: [
      {
        path: 'list',
        name: 'ConcertList',
        component: () => import('@/views/concert/list.vue'),
        meta: { title: '演唱会列表' }
      },
      {
        path: 'detail/:id',
        name: 'ConcertDetail',
        component: () => import('@/views/concert/detail.vue'),
        hidden: true,
        meta: { title: '演唱会详情', activeMenu: '/concert/list' }
      },
      {
        path: 'seat/:showId',
        name: 'SeatSelect',
        component: () => import('@/views/concert/seat.vue'),
        hidden: true,
        meta: { title: '选座购票', activeMenu: '/concert/list' }
      }
    ]
  },

  {
    path: '/order',
    component: Layout,
    redirect: '/order/my',
    name: 'Order',
    meta: { title: '我的订单', icon: 'List' },
    children: [
      {
        path: 'my',
        name: 'MyOrders',
        component: () => import('@/views/order/my.vue'),
        meta: { title: '我的订单' }
      },
      {
        path: 'detail/:id',
        name: 'OrderDetail',
        component: () => import('@/views/order/detail.vue'),
        hidden: true,
        meta: { title: '订单详情', activeMenu: '/order/my' }
      }
    ]
  },

  {
    path: '/ticket',
    component: Layout,
    name: 'Ticket',
    meta: { title: '取票码', icon: 'Ticket' },
    children: [
      {
        path: 'my-codes',
        name: 'MyTicketCodes',
        component: () => import('@/views/ticket/my-codes.vue'),
        meta: { title: '我的取票码' }
      }
    ]
  },

  {
    path: '/profile',
    component: Layout,
    name: 'Profile',
    meta: { title: '个人中心', icon: 'User' },
    children: [
      {
        path: 'index',
        name: 'ProfileIndex',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人信息' }
      }
    ]
  }
]

/**
 * asyncRoutes
 * 需要权限控制的路由，根据用户角色动态加载
 */
export const asyncRoutes = [
  // ===================== 管理后台模块 =====================
  {
    path: '/admin',
    component: Layout,
    redirect: '/admin/dashboard',
    name: 'Admin',
    meta: { title: '管理后台', icon: 'Setting', roles: ['ADMIN'] },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/dashboard/index.vue'),
        meta: { title: '数据看板', icon: 'dashboard' }
      },
      {
        path: 'concert',
        name: 'AdminConcert',
        component: () => import('@/views/admin/concert/index.vue'),
        meta: { title: '演唱会管理' }
      },
      {
        path: 'artist',
        name: 'AdminArtist',
        component: () => import('@/views/admin/artist/index.vue'),
        meta: { title: '艺人管理' }
      },
      {
        path: 'show',
        name: 'AdminShow',
        component: () => import('@/views/admin/show/index.vue'),
        meta: { title: '场次管理' }
      },
      {
        path: 'venue',
        name: 'AdminVenue',
        component: () => import('@/views/admin/venue/index.vue'),
        meta: { title: '场馆管理' }
      },
      {
        path: 'ticket-type',
        name: 'AdminTicketType',
        component: () => import('@/views/admin/ticket-type/index.vue'),
        meta: { title: '票种管理' }
      },
      {
        path: 'ticket-verify',
        name: 'AdminTicketVerify',
        component: () => import('@/views/admin/ticket/verify.vue'),
        meta: { title: '取票核销' }
      },
      {
        path: 'order',
        name: 'AdminOrder',
        component: () => import('@/views/admin/order/index.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'user',
        name: 'AdminUser',
        component: () => import('@/views/admin/user/index.vue'),
        meta: { title: '用户管理' }
      }
    ]
  },

  // 404 must be placed at the end !!!
  { path: '/:pathMatch(.*)*', redirect: '/404', hidden: true }
]

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: constantRoutes
})

export function resetRouter() {
  const newRouter = createRouter({
    history: createWebHistory(),
    scrollBehavior: () => ({ top: 0 }),
    routes: constantRoutes
  })
  router.matcher = newRouter.matcher // reset router
}

export default router
