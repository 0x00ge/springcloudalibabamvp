import type { RouteRecordRaw } from 'vue-router'

// routes 单独抽出，index.ts 只负责创建 router 和挂载守卫。
export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/home',
  },
  {
    path: '/login',
    name: 'Login',
    // 路由懒加载，登录页代码会被拆成单独 chunk。
    component: () => import('@/views/Login.vue'),
  },
  {
    // 后台首页，承载整体后台布局。
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    redirect: '/home/user',
    meta: {
      // 需要登录后才能访问，路由守卫会检查 token。
      requiresAuth: true,
      title: '首页',
    },
    children: [
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/User.vue'),
        meta: {
          requiresAuth: true,
          title: '用户管理',
        },
      },
      {
        path: 'department',
        name: 'Department',
        component: () => import('@/views/Department.vue'),
        meta: {
          requiresAuth: true,
          title: '部门管理',
        },
      },
    ],
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
  },
]
