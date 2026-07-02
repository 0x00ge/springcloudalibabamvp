import { Button, Form, Input, message, Segmented } from 'antd'
import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

import { registerCodeByPhone } from '@/api/apiAuth'
import { authStore } from '@/stores/authStore'
import type { CurrentAuthParams, LoginParams } from '@/types/authTypes'
import './Login.css'

type LoginOrRegister = 'login' | 'register'

interface RegisterForm {
  name: string
  phone: string
  smsCode: string
  password: string
  confirmPassword: string
}

export default function Login() {
  const [loginForm] = Form.useForm<LoginParams>()
  const [registerForm] = Form.useForm<RegisterForm>()
  const [loading, setLoading] = useState(false)
  const [sendCodeLoading, setSendCodeLoading] = useState(false)
  const [smsCountdown, setSmsCountdown] = useState(0)
  const [mode, setMode] = useState<LoginOrRegister>('login')

  const navigate = useNavigate()
  const location = useLocation()

  // 登录流程：
  // 1. 先触发表单校验；
  // 2. 校验通过后调用 authStore.loginAction；
  // 3. authStore 内部会请求真实 /auth/login，并保存响应体中的 accessToken；
  // 4. refreshToken 不进入前端 JS，由后端通过 HttpOnly Cookie 写入浏览器。
  const handleLogin = async () => {
    const values = await loginForm.validateFields()
    setLoading(true)

    try {
      await authStore.loginAction(values)
      message.success('登录成功')
      handleRedirectToHome()
    } finally {
      setLoading(false)
    }
  }

  // 注册：
  // 1. 先校验手机号、验证码、密码、确认密码、姓名；
  // 2. 前端再校验一次两次密码是否一致；
  // 3. 调用 /auth/register；
  // 4. 注册成功后不自动登录，而是切回登录表单，让用户显式登录获取 token。
  const handleRegister = async () => {
    const values = await registerForm.validateFields()

    if (values.password !== values.confirmPassword) {
      message.error('两次输入的密码不一致')
      return
    }

    setLoading(true)

    try {
      await authStore.registerAction(values as CurrentAuthParams)
      loginForm.setFieldsValue({
        phone: values.phone,
        password: '',
      })
      setMode('login')
      message.success('注册成功，请登录')
    } finally {
      setLoading(false)
    }
  }

  // 验证码
  // 1. 只校验手机号字段，不要求用户提前填完整注册表单；
  // 2. 调用 /auth/register/code；
  // 3. 成功后启动前端倒计时。
  const handleSmsCode = async () => {
    if (smsCountdown > 0) return

    await registerForm.validateFields(['phone'])
    setSendCodeLoading(true)

    try {
      const phone = registerForm.getFieldValue('phone')
      await registerCodeByPhone({ phone })
      message.success('验证码已发送')
      setSmsCountdown(60)
    } finally {
      setSendCodeLoading(false)
    }
  }

  // 登录成功后的跳转目标：
  // 1. 如果路由守卫曾经把用户拦到 /login，会带上 redirect；
  // 2. 登录成功后优先回到 redirect；
  // 3. 没有 redirect 时默认进入 /home。
  const handleRedirectToHome = () => {
    const searchParams = new URLSearchParams(location.search)
    const redirect = searchParams.get('redirect')

    navigate(redirect || '/home', { replace: true })
  }

  // 离开登录页时清理倒计时定时器，避免内存泄漏或重复倒计时。
  useEffect(() => {
    if (smsCountdown <= 0) return undefined

    const timer = window.setInterval(() => {
      setSmsCountdown((current) => Math.max(current - 1, 0))
    }, 1000)

    return () => {
      window.clearInterval(timer)
    }
  }, [smsCountdown])

  return (
    <div className="login-page">
      <div className="login-panel">
        <div className="login-brand">
          <h1>MVP后台管理系统</h1>
        </div>

        <Segmented<LoginOrRegister>
          className="auth-mode"
          value={mode}
          options={[
            { label: '登录', value: 'login' },
            { label: '注册', value: 'register' },
          ]}
          onChange={setMode}
        />

        {mode === 'login' ? (
          /* 登录表单 */
          /* 原先登录表单规则注释保留：rules={loginRules} */
          <Form className="login-form" form={loginForm} size="large" onFinish={handleLogin}>
            <Form.Item
              name="phone"
              rules={[
                { required: true, message: '请输入手机号' },
                { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
              ]}
            >
              <Input placeholder="请输入手机号" />
            </Form.Item>

            <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
              <Input.Password placeholder="请输入密码" />
            </Form.Item>

            <Button className="login-button" type="primary" loading={loading} htmlType="submit">
              登录
            </Button>
          </Form>
        ) : (
          /* 注册表单 */
          <Form className="login-form" form={registerForm} size="large" onFinish={handleRegister}>
            <Form.Item
              name="name"
              rules={[
                { required: true, message: '请输入姓名' },
                { max: 50, message: '姓名长度不能超过 50 位' },
              ]}
            >
              <Input placeholder="请输入姓名" />
            </Form.Item>

            <Form.Item
              name="phone"
              rules={[
                { required: true, message: '请输入手机号' },
                { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
              ]}
            >
              <Input placeholder="请输入手机号" />
            </Form.Item>

            <div className="sms-code-row">
              <Form.Item
                name="smsCode"
                rules={[
                  { required: true, message: '请输入验证码' },
                  { pattern: /^\d{6}$/, message: '验证码为 6 位数字' },
                ]}
              >
                <Input placeholder="请输入验证码" />
              </Form.Item>
              <Button
                className="sms-code-button"
                loading={sendCodeLoading}
                disabled={smsCountdown > 0}
                onClick={handleSmsCode}
              >
                {smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码'}
              </Button>
            </div>

            <Form.Item
              name="password"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 6, message: '密码至少 6 位' },
              ]}
            >
              <Input.Password placeholder="请输入密码" />
            </Form.Item>

            <Form.Item
              name="confirmPassword"
              rules={[
                { required: true, message: '请再次输入密码' },
                { min: 6, message: '密码至少 6 位' },
              ]}
            >
              <Input.Password placeholder="请再次输入密码" />
            </Form.Item>

            <Button className="login-button" type="primary" loading={loading} htmlType="submit">
              注册
            </Button>
          </Form>
        )}
      </div>
    </div>
  )
}
