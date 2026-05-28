// Pinia stores 在各模块中直接导入使用，无需集中注册
// 使用时：import { useUserStore } from '@/store/modules/user'
export { useUserStore } from './modules/user'
export { useAppStore } from './modules/app'
export { useSettingsStore, usePermissionStore } from './modules/settings'
