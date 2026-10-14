<template>
  <span>
    <el-icon v-if="elementIcon" class="sub-el-icon"><component :is="elementIcon" /></el-icon>
    <svg-icon v-else-if="icon" :icon-class="icon" />
    <span v-if="title">{{ title }}</span>
  </span>
</template>

<script>
import { elementPlusIcons } from '@/icons/element-plus'

export default {
  name: 'MenuItem',
  props: {
    icon: {
      type: String,
      default: ''
    },
    title: {
      type: String,
      default: ''
    }
  },
  computed: {
    /**
     * 路由 meta.icon 优先匹配已登记的 Element Plus 图标，未登记的交由本地 svg 图标集渲染
     */
    elementIcon() {
      return this.icon && elementPlusIcons[this.icon] ? this.icon : ''
    }
  }
}
</script>

<style scoped>
.sub-el-icon {
  color: currentColor;
  width: 1em;
  height: 1em;
}
</style>
