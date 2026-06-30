<script setup lang="ts">
import {onBeforeUnmount, reactive, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage, type FormInstance, type FormRules} from 'element-plus'
import {Cellphone, Lock, Message, User} from '@element-plus/icons-vue'

import {registerCodeByPhone} from '@/api/apiAuth.js'
import {useAuthStore} from '@/stores/authStore.ts'

interface LoginForm {
  phone: string
  password: string
}

interface RegisterForm {
  name: string
  phone: string
  smsCode: string
  password: string
  confirmPassword: string
}

type LoginOrRegister = 'login' | 'register'

// router 用于登录成功后跳转，route 用于读取 redirect 参数。
const router = useRouter()
const route = useRoute()

// 登录、注册、刷新、登出都属于认证职责，统一从 authStore 进入。
const authStore = useAuthStore()

// Element Plus 表单实例，用于手动触发表单校验。
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

// loading 控制登录/注册主按钮；sendCodeLoading 单独控制验证码按钮。
const loading = ref(false)
const sendCodeLoading = ref(false)

// 短信倒计时，只影响前端按钮状态；真正的验证码有效期以后端 Redis TTL 为准。
const smsCountdown = ref(0)

// 登录页通过 segmented 在登录表单和注册表单之间切换。
const isLoginOrRegister = ref<LoginOrRegister>('login')

// 保存倒计时定时器 ID，组件卸载时清理，避免离开页面后定时器继续运行。
let countdownTimer: number | undefined

// 登录表单响应式数据，和模板中的 el-form :model 绑定。
const loginForm = reactive<LoginForm>({
  phone: '',
  password: '',
})

// 注册表单响应式数据，字段和后端 /auth/register 参数保持一致。
const registerForm = reactive<RegisterForm>({
  phone: '',
  smsCode: '',
  password: '',
  confirmPassword: '',
  name: '',
})

// 登录校验
const loginRules = reactive<FormRules<RegisterForm>>({
  phone: [
    {required: true, message: '请输入手机号', trigger: 'blur'},
    {pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur'},
  ],
  password: [{required: true, message: '请输入密码', trigger: 'blur'}],
})

// 注册校验
const registerRules = reactive<FormRules<RegisterForm>>({
  phone: [
    {required: true, message: '请输入手机号', trigger: 'blur'},
    {pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur'},
  ],
  smsCode: [
    {required: true, message: '请输入验证码', trigger: 'blur'},
    {pattern: /^\d{6}$/, message: '验证码为 6 位数字', trigger: 'blur'},
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, message: '密码至少 6 位', trigger: 'blur'},
  ],
  confirmPassword: [
    {required: true, message: '请再次输入密码', trigger: 'blur'},
    {min: 6, message: '密码至少 6 位', trigger: 'blur'},
  ],
  name: [
    {required: true, message: '请输入姓名', trigger: 'blur'},
    {max: 50, message: '姓名长度不能超过 50 位', trigger: 'blur'},
  ],
})

// 登录成功后的跳转目标：
// 1. 如果路由守卫曾经把用户拦到 /login，会带上 redirect；
// 2. 登录成功后优先回到 redirect；
// 3. 没有 redirect 时默认进入 /home。
const redirectToTarget = () => {
  const redirect = Array.isArray(route.query.redirect)
      ? route.query.redirect[0]
      : route.query.redirect

  router.replace((redirect as string) || '/home')
}

// 登录流程：
// 1. 先触发表单校验；
// 2. 校验通过后调用 authStore.loginAction；
// 3. authStore 内部会请求真实 /auth/login，并保存响应体中的 accessToken；
// 4. refreshToken 不进入前端 JS，由后端通过 HttpOnly Cookie 写入浏览器。
const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate()
  loading.value = true

  try {
    // 登录逻辑交给 authStore 统一处理：接口请求、保存 token 都放在一起。
    await authStore.loginAction({
      phone: loginForm.phone,
      password: loginForm.password,
    })

    ElMessage.success('登录成功')
    redirectToTarget()
  } finally {
    loading.value = false
  }
}

// 开始验证码按钮倒计时。
//
// 注意这里只是防止用户频繁点击按钮；真正防刷、验证码有效期和验证码校验都应该以后端为准。
const startSmsCountdown = () => {
  smsCountdown.value = 60
  window.clearInterval(countdownTimer)

  countdownTimer = window.setInterval(() => {
    smsCountdown.value -= 1

    if (smsCountdown.value <= 0) {
      window.clearInterval(countdownTimer)
      countdownTimer = undefined
    }
  }, 1000)
}

// 发送注册验证码流程：
// 1. 只校验手机号字段，不要求用户提前填完整注册表单；
// 2. 调用真实 /auth/register/code；
// 3. 成功后启动前端倒计时。
const handleSendRegisterCode = async () => {
  if (!registerFormRef.value || smsCountdown.value > 0) return

  await registerFormRef.value.validateField('phone')
  sendCodeLoading.value = true

  try {
    await registerCodeByPhone({
      phone: registerForm.phone,
    })

    ElMessage.success('验证码已发送')
    startSmsCountdown()
  } finally {
    sendCodeLoading.value = false
  }
}

// 注册流程：
// 1. 先校验手机号、验证码、密码、确认密码、姓名；
// 2. 前端再校验一次两次密码是否一致；
// 3. 调用真实 /auth/register；
// 4. 注册成功后不自动登录，而是切回登录表单，让用户显式登录获取 token。
const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate()
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  loading.value = true

  try {
    await authStore.registerAction({
      phone: registerForm.phone,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      smsCode: registerForm.smsCode,
      name: registerForm.name,
    })

    loginForm.phone = registerForm.phone
    loginForm.password = ''
    isLoginOrRegister.value = 'login'
    ElMessage.success('注册成功，请登录')
  } finally {
    loading.value = false
  }
}

// 回车提交
const handleSubmit = () => {
  if (isLoginOrRegister.value === 'login') {
    handleLogin()
    return
  }

  handleRegister()
}

// 离开登录页时清理倒计时定时器，避免内存泄漏或重复倒计时。
onBeforeUnmount(() => {
  window.clearInterval(countdownTimer)
})
</script>

<template>
  <div class="login-page">

    <!--  登录面板  -->
    <div class="login-panel">
      <div class="login-brand">
        <h1>MVP后台管理系统</h1>
      </div>

      <el-segmented class="auth-mode"
                    v-model="isLoginOrRegister"
                    :options="[
                      { label: '登录', value: 'login' },
                      { label: '注册', value: 'register' },
                    ]"
      />

      <el-form class="login-form"
               v-if="isLoginOrRegister === 'login'"
               ref="loginFormRef"
               :model="loginForm"
               :rules="loginRules"
               size="large"
               @keyup.enter="handleSubmit"
      >
        <el-form-item prop="phone">
          <el-input v-model="loginForm.phone" placeholder="请输入手机号" :prefix-icon="Cellphone"/>
        </el-form-item>

        <el-form-item prop="password">
          <el-input v-model="loginForm.password" placeholder="请输入密码" :prefix-icon="Lock"
                    type="password" show-password/>
        </el-form-item>

        <el-button class="login-button" type="primary" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>

      <!-- 注册表单 -->
      <el-form class="login-form"
               v-else
               ref="registerFormRef"
               :model="registerForm"
               :rules="registerRules"
               size="large"
               @keyup.enter="handleSubmit"
      >
        <el-form-item prop="phone">
          <!-- 注册手机号也是后续登录账号。 -->
          <el-input v-model="registerForm.phone" placeholder="请输入手机号" :prefix-icon="Cellphone"/>
        </el-form-item>

        <el-form-item prop="smsCode">
          <!-- 验证码输入框和发送按钮并排展示，倒计时期间禁止重复发送。 -->
          <div class="sms-code-row">
            <el-input
                v-model="registerForm.smsCode"
                placeholder="请输入验证码"
                :prefix-icon="Message"
            />
            <el-button
                class="sms-code-button"
                :loading="sendCodeLoading"
                :disabled="smsCountdown > 0"
                @click="handleSendRegisterCode"
            >
              {{ smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item prop="password">
          <!-- 密码明文只存在于当前表单，提交后由后端 BCrypt 加密入库。 -->
          <el-input
              v-model="registerForm.password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              type="password"
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <!-- 确认密码用于减少误输入，前后端都会校验两次密码一致。 -->
          <el-input
              v-model="registerForm.confirmPassword"
              placeholder="请再次输入密码"
              :prefix-icon="Lock"
              show-password
              type="password"
          />
        </el-form-item>

        <el-form-item prop="name">
          <!-- 用户名称对应后端 User.name 字段。 -->
          <el-input v-model="registerForm.name" placeholder="请输入姓名" :prefix-icon="User"/>
        </el-form-item>

        <el-button class="login-button" type="primary" :loading="loading" @click="handleRegister">
          注册
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped lang="less">
.login-page {
  /* 登录页内部统一盒模型，配合全局清零样式保证面板宽度稳定。 */
  box-sizing: border-box;
  /* 登录页独立于后台布局，铺满整个视口。 */
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
  background: #eef2f8;
}

.login-page *,
.login-page *::before,
.login-page *::after {
  /* 只在登录页范围内继承 border-box，不修改 style.less。 */
  box-sizing: border-box;
}

.login-panel {
  /* 登录表单容器固定最大宽度，避免桌面端过宽。 */
  width: min(100%, 420px);
  padding: 32px;
  background: #ffffff;
  border: 1px solid #e1e6ef;
  border-radius: 8px;
  box-shadow: 0 18px 45px rgb(15 23 42 / 10%);
}

.login-brand {
  /* 品牌区横向排列 logo 和标题。 */
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
}

.brand-mark {
  /* 简单文字 logo，后续可替换为真实图片或图标。 */
  display: grid;
  width: 44px;
  height: 44px;
  color: #ffffff;
  font-size: 22px;
  font-weight: 700;
  place-items: center;
  background: #409eff;
  border-radius: 8px;
}

.login-brand h1 {
  margin: 0;
  color: #111827;
  font-size: 24px;
  line-height: 1.2;
}

.login-brand p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 14px;
}

.auth-mode {
  width: 100%;
  margin-bottom: 22px;
}

.login-form {
  width: 100%;
}

.sms-code-row {
  display: grid;
  width: 100%;
  grid-template-columns: minmax(0, 1fr) 118px;
  gap: 10px;
}

.sms-code-button {
  width: 118px;
}

.login-button {
  width: 100%;
}

@media (max-width: 480px) {
  .login-page {
    padding: 16px;
  }

  .login-panel {
    padding: 24px;
  }

  .sms-code-row {
    grid-template-columns: minmax(0, 1fr) 104px;
  }

  .sms-code-button {
    width: 104px;
    padding-right: 8px;
    padding-left: 8px;
  }
}
</style>
