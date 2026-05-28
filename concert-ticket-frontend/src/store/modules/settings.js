import { defineStore } from 'pinia'
import defaultSettings from '@/settings'
import { asyncRoutes, constantRoutes } from '@/router'

const { showSettings, fixedHeader, sidebarLogo } = defaultSettings

function hasPermission(roles, route) {
  if (route.meta && route.meta.roles) {
    return roles.some(role => route.meta.roles.includes(role))
  } else {
    return true
  }
}

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

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    showSettings: showSettings,
    fixedHeader: fixedHeader,
    sidebarLogo: sidebarLogo
  }),

  actions: {
    changeSetting({ key, value }) {
      if (Object.prototype.hasOwnProperty.call(this, key)) {
        this[key] = value
      }
    }
  }
})

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    routes: [],
    addedRoutes: []
  }),

  actions: {
    generateRoutes(roles) {
      let accessedRoutes
      if (roles.includes('ADMIN')) {
        accessedRoutes = asyncRoutes || []
      } else {
        accessedRoutes = filterAsyncRoutes(asyncRoutes, roles)
      }
      this.addedRoutes = accessedRoutes
      this.routes = constantRoutes.concat(accessedRoutes)
      return accessedRoutes
    }
  }
})
