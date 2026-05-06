import { asyncRoutes, constantRoutes } from '@/router'

/**
 * 通过 meta.roles 判断用户是否拥有该路由的权限
 * @param roles 用户角色列表
 * @param route 路由配置
 */
function hasPermission(roles, route) {
  if (route.meta && route.meta.roles) {
    return roles.some(role => route.meta.roles.includes(role))
  } else {
    return true
  }
}

/**
 * 递归过滤异步路由，只保留有权限的路由
 * @param routes asyncRoutes
 * @param roles 用户角色列表
 */
export function filterAsyncRoutes(routes, roles) {
  const res = []

  routes.forEach(route => {
    const tmp = { ...route }
    if (hasPermission(roles, tmp)) {
      if (tmp.children) {
        tmp.children = filterAsyncRoutes(tmp.children, roles)
      }
      res.push(tmp)
    }
  })

  return res
}

const state = {
  routes: [],
  addedRoutes: []
}

const mutations = {
  SET_ROUTES: (state, routes) => {
    state.addedRoutes = routes
    state.routes = constantRoutes.concat(routes)
  }
}

const actions = {
  generateRoutes({ commit }, roles) {
    return new Promise(resolve => {
      let accessedRoutes
      // 如果角色中包含 ADMIN，则拥有所有异步路由的权限
      if (roles.includes('ADMIN')) {
        accessedRoutes = asyncRoutes || []
      } else {
        // 否则根据角色过滤
        accessedRoutes = filterAsyncRoutes(asyncRoutes, roles)
      }
      commit('SET_ROUTES', accessedRoutes)
      resolve(accessedRoutes)
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
