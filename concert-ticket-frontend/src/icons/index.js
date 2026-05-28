// src/icons/index.js
import SvgIcon from '@/components/SvgIcon/index.vue' // 确保该组件存在

// 导出安装函数，供 main.js 调用
export default function installSvgIcon(app) {
  // 全局注册组件
  app.component('svg-icon', SvgIcon)
  // 加载所有 svg 文件（根据实际路径调整）
  const svgFiles = import.meta.globEager('./svg/*.svg') // Vite 方式
  // 或者使用 require.context 的替代方案
}
