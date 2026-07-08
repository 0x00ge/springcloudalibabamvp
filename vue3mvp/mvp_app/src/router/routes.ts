import type { RouteRecordRaw } from 'vue-router'

// routes 单独抽出，index.ts 只负责创建 router 和挂载守卫。
export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/home',
  },
  {
    path: '/login',
    // 路由懒加载，登录页代码会被拆成单独 chunk。
    component: () => import('@/views/Login.vue'),
    name: 'Login',
  },
  {
    path: '/home',
    component: () => import('@/views/Home.vue'),
    name: 'Home',
    redirect: '/home/user',
    meta: {
      // 路由守卫会检查 token，非继承。
      requiresAuth: true,
      title: '首页',
    },
    children: [
      {
        path: 'user',
        component: () => import('@/views/User.vue'),
        name: 'User',
        meta: {
          requiresAuth: true,
          title: '用户管理',
        },
      },
      {
        path: 'menu',
        component: () => import('@/views/Menu.vue'),
        name: 'Menu',
        meta: {
          requiresAuth: true,
          title: '菜单管理',
        },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
  },
  {
    path: '/404',
    component: () => import('@/views/NotFound.vue'),
    name: 'NotFound',
  },
]
