import SvgIcon from '@/components/SvgIcon/index.vue'

export default function installSvgIcon(app) {
  app.component('svg-icon', SvgIcon)
  // Vite 5: 使用 import.meta.glob 替代已移除的 globEager
  const svgModules = import.meta.glob('./svg/*.svg', { eager: true })
  // svg 文件会被 vite-plugin-svg-icons 处理，这里只需确保它们被加载
  Object.keys(svgModules).forEach(() => {
    // 触发模块加载即可，svg 注册由插件完成
  })
}
