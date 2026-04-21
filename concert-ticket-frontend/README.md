---------------项目结构------------------
src/
├── api/                    # 接口请求
│   ├── concert.js          # 演出相关接口
│   ├── order.js            # 订单相关接口
│   └── user.js             # 用户相关接口
├── components/             # 通用组件
│   ├── SeatSelector/       # 座位选择器组件（核心）
│   └── ...
├── views/                  # 页面视图
│   ├── concert/            # 演出模块
│   │   ├── List.vue        # 演出列表页
│   │   ├── Detail.vue      # 演出详情页
│   │   └── SelectSeat.vue  # 选座页
│   ├── order/              # 订单模块
│   │   ├── Confirm.vue     # 订单确认页
│   │   └── List.vue        # 我的订单页
│   └── user/               # 用户模块
│       ├── Login.vue       # 登录页
│       └── Register.vue    # 注册页
├── router/                 # 路由配置
└── store/                  # 状态管理（Vuex）
