import { useAppStore } from '@/store'

const { body } = document
const WIDTH = 992

export default {
  watch: {
    $route(route) {
      const appStore = useAppStore()
      if (appStore.device === 'mobile' && appStore.sidebar.opened) {
        appStore.closeSideBar(false)
      }
    }
  },
  beforeMount() {
    window.addEventListener('resize', this.$_resizeHandler)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.$_resizeHandler)
  },
  mounted() {
    const isMobile = this.$_isMobile()
    if (isMobile) {
      const appStore = useAppStore()
      appStore.toggleDevice('mobile')
      appStore.closeSideBar(true)
    }
  },
  methods: {
    $_isMobile() {
      const rect = body.getBoundingClientRect()
      return rect.width - 1 < WIDTH
    },
    $_resizeHandler() {
      if (!document.hidden) {
        const isMobile = this.$_isMobile()
        const appStore = useAppStore()
        appStore.toggleDevice(isMobile ? 'mobile' : 'desktop')

        if (isMobile) {
          appStore.closeSideBar(true)
        }
      }
    }
  }
}
