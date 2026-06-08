<script setup lang="ts">
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Cellphone, Lock, Message, User } from '@element-plus/icons-vue'

import { sendRegisterSmsCode } from '@/api/authApi.ts'
import { useUserStore } from '@/stores/userStore.ts'

interface LoginForm {
  phone: string
  password: string
}

interface RegisterForm {
  phone: string
  smsCode: string
  password: string
  confirmPassword: string
  name: string
}

type AuthMode = 'login' | 'register'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const sendCodeLoading = ref(false)
const smsCountdown = ref(0)
const authMode = ref<AuthMode>('login')
let countdownTimer: number | undefined

const loginForm = reactive<LoginForm>({
  phone: '',
  password: '',
})

const registerForm = reactive<RegisterForm>({
  phone: '',
  smsCode: '',
  password: '',
  confirmPassword: '',
  name: '',
})

const loginRules: FormRules<LoginForm> = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registerRules: FormRules<RegisterForm> = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  smsCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为 6 位数字', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 50, message: '姓名长度不能超过 50 位', trigger: 'blur' },
  ],
}

const redirectToTarget = () => {
  const redirect = Array.isArray(route.query.redirect)
    ? route.query.redirect[0]
    : route.query.redirect

  router.replace((redirect as string) || '/home')
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate()
  loading.value = true

  try {
    // 登录逻辑交给 Pinia 的 userStore 统一处理：接口请求、保存 token 都放在一起。
    await userStore.loginAction({
      phone: loginForm.phone,
      password: loginForm.password,
    })

    ElMessage.success('登录成功')
    redirectToTarget()
  } finally {
    loading.value = false
  }
}

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

const handleSendRegisterCode = async () => {
  if (!registerFormRef.value || smsCountdown.value > 0) return

  await registerFormRef.value.validateField('phone')
  sendCodeLoading.value = true

  try {
    await sendRegisterSmsCode({
      phone: registerForm.phone,
    })

    ElMessage.success('验证码已发送')
    startSmsCountdown()
  } finally {
    sendCodeLoading.value = false
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate()
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  loading.value = true

  try {
    await userStore.registerAction({
      phone: registerForm.phone,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      smsCode: registerForm.smsCode,
      name: registerForm.name,
    })

    loginForm.phone = registerForm.phone
    loginForm.password = ''
    authMode.value = 'login'
    ElMessage.success('注册成功，请登录')
  } finally {
    loading.value = false
  }
}

const handleSubmit = () => {
  if (authMode.value === 'login') {
    handleLogin()
    return
  }

  handleRegister()
}

onBeforeUnmount(() => {
  window.clearInterval(countdownTimer)
})
</script>

<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-brand">
        <span class="brand-mark">V</span>
        <div>
          <h1>Vue3 MVP</h1>
          <p>后台管理系统</p>
        </div>
      </div>

      <el-segmented
        v-model="authMode"
        class="auth-mode"
        :options="[
          { label: '登录', value: 'login' },
          { label: '注册', value: 'register' },
        ]"
      />

      <el-form
        v-if="authMode === 'login'"
        ref="loginFormRef"
        class="login-form"
        :model="loginForm"
        :rules="loginRules"
        size="large"
        @keyup.enter="handleSubmit"
      >
        <el-form-item prop="phone">
          <el-input v-model="loginForm.phone" placeholder="请输入手机号" :prefix-icon="Cellphone" />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            type="password"
          />
        </el-form-item>

        <el-button class="login-button" type="primary" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>

      <el-form
        v-else
        ref="registerFormRef"
        class="login-form"
        :model="registerForm"
        :rules="registerRules"
        size="large"
        @keyup.enter="handleSubmit"
      >
        <el-form-item prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号" :prefix-icon="Cellphone" />
        </el-form-item>

        <el-form-item prop="smsCode">
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
          <el-input
            v-model="registerForm.password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            type="password"
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            placeholder="请再次输入密码"
            :prefix-icon="Lock"
            show-password
            type="password"
          />
        </el-form-item>

        <el-form-item prop="name">
          <el-input v-model="registerForm.name" placeholder="请输入姓名" :prefix-icon="User" />
        </el-form-item>

        <el-button class="login-button" type="primary" :loading="loading" @click="handleRegister">
          注册
        </el-button>
      </el-form>
    </section>
  </main>
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
