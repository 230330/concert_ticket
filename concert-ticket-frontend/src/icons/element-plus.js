import {
  CaretBottom, Search, Tickets, Money, Headset, UserFilled, View, Ticket, Check,
  Location, OfficeBuilding, Clock, Iphone, Lock, Message, DocumentCopy, Edit,
  CreditCard, CircleCheck, CircleClose, Plus, Refresh, List, User, Setting
} from '@element-plus/icons-vue'

/**
 * 项目实际使用到的 Element Plus 图标
 *
 * Element Plus 2 已移除字体图标（el-icon-xxx 之类的 class），改为 SVG 组件，
 * 因此这里集中登记，main.js 统一全局注册，模板里可直接按名引用：
 *   - 组件写法：<el-icon><Search /></el-icon>
 *   - 属性写法：<el-button icon="Search" />（EP2 通过 resolveDynamicComponent 解析名字）
 *
 * 新增图标时在此登记即可，不要改回字体图标 class。
 */
export const elementPlusIcons = {
  CaretBottom, Search, Tickets, Money, Headset, UserFilled, View, Ticket, Check,
  Location, OfficeBuilding, Clock, Iphone, Lock, Message, DocumentCopy, Edit,
  CreditCard, CircleCheck, CircleClose, Plus, Refresh, List, User, Setting
}

export default elementPlusIcons
