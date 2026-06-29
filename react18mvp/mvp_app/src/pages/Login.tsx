import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { LockOutlined, MessageOutlined, MobileOutlined, UserOutlined } from '@ant-design/icons'
import { Button, Form, Input, Segmented, message } from 'antd'

import { registerCodeByPhone } from '@/api/apiAuth'
import { useAuth } from '@/context/AuthContext'
import { getAccessToken } from '@/context/authRuntime'
import type { CurrentAuthParams, LoginParams } from '@/types/authTypes'

type AuthMode = 'login' | 'register'

interface RegisterFormValues {
  phone: string
  smsCode: string
  password: string
  confirmPassword: string
  name: string
}

const phoneRules = [
  { required: true, message: '请输入手机号' },
  { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
]

export default function Login() {
  const [authMode, setAuthMode] = useState<AuthMode>('login')
  const [loading, setLoading] = useState(false)
  const [sendCodeLoading, setSendCodeLoading] = useState(false)
  const [smsCountdown, setSmsCountdown] = useState(0)

  const [loginForm] = Form.useForm<LoginParams>()
  const [registerForm] = Form.useForm<RegisterFormValues>()

  const auth = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  useEffect(() => {
    if (getAccessToken()) {
      navigate(searchParams.get('redirect') || '/home', { replace: true })
    } else {
      auth.clearLoginState()
    }
  }, [auth, navigate, searchParams])

  useEffect(() => {
    if (smsCountdown <= 0) return undefined

    const timer = window.setInterval(() => {
      setSmsCountdown((value) => {
        if (value <= 1) {
          window.clearInterval(timer)
          return 0
        }

        return value - 1
      })
    }, 1000)

    return () => window.clearInterval(timer)
  }, [smsCountdown])

  const redirectToTarget = () => {
    navigate(searchParams.get('redirect') || '/home', { replace: true })
  }

  const handleLogin = async (values: LoginParams) => {
    setLoading(true)

    try {
      await auth.loginAction(values)
      message.success('登录成功')
      redirectToTarget()
    } finally {
      setLoading(false)
    }
  }

  const handleSendRegisterCode = async () => {
    if (smsCountdown > 0) return

    const { phone } = await registerForm.validateFields(['phone'])
    setSendCodeLoading(true)

    try {
      await registerCodeByPhone({ phone })
      message.success('验证码已发送')
      setSmsCountdown(60)
    } finally {
      setSendCodeLoading(false)
    }
  }

  const handleRegister = async (values: RegisterFormValues) => {
    if (values.password !== values.confirmPassword) {
      message.error('两次输入的密码不一致')
      return
    }

    setLoading(true)

    try {
      await auth.registerAction(values as CurrentAuthParams)
      loginForm.setFieldsValue({ phone: values.phone, password: '' })
      setAuthMode('login')
      message.success('注册成功，请登录')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-panel">
        <div className="login-brand">
          <span className="brand-mark">R</span>
          <div>
            <h1>React18 MVP</h1>
            <p>后台管理系统</p>
          </div>
        </div>

        <Segmented
          className="auth-mode"
          block
          value={authMode}
          options={[
            { label: '登录', value: 'login' },
            { label: '注册', value: 'register' },
          ]}
          onChange={(value) => setAuthMode(value as AuthMode)}
        />

        {authMode === 'login' ? (
          <Form form={loginForm} className="login-form" size="large" onFinish={handleLogin}>
            <Form.Item name="phone" rules={phoneRules}>
              <Input prefix={<MobileOutlined />} placeholder="请输入手机号" />
            </Form.Item>
            <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
              <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" />
            </Form.Item>
            <Button className="login-button" type="primary" htmlType="submit" loading={loading}>
              登录
            </Button>
          </Form>
        ) : (
          <Form form={registerForm} className="login-form" size="large" onFinish={handleRegister}>
            <Form.Item name="phone" rules={phoneRules}>
              <Input prefix={<MobileOutlined />} placeholder="请输入手机号" />
            </Form.Item>
            <Form.Item
              name="smsCode"
              rules={[
                { required: true, message: '请输入验证码' },
                { pattern: /^\d{6}$/, message: '验证码为 6 位数字' },
              ]}
            >
              <Input
                prefix={<MessageOutlined />}
                placeholder="请输入验证码"
                addonAfter={
                  <Button
                    className="sms-code-button"
                    type="link"
                    loading={sendCodeLoading}
                    disabled={smsCountdown > 0}
                    onClick={handleSendRegisterCode}
                  >
                    {smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码'}
                  </Button>
                }
              />
            </Form.Item>
            <Form.Item
              name="password"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 6, message: '密码至少 6 位' },
              ]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" />
            </Form.Item>
            <Form.Item
              name="confirmPassword"
              rules={[
                { required: true, message: '请再次输入密码' },
                { min: 6, message: '密码至少 6 位' },
              ]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="请再次输入密码" />
            </Form.Item>
            <Form.Item
              name="name"
              rules={[
                { required: true, message: '请输入姓名' },
                { max: 50, message: '姓名长度不能超过 50 位' },
              ]}
            >
              <Input prefix={<UserOutlined />} placeholder="请输入姓名" />
            </Form.Item>
            <Button className="login-button" type="primary" htmlType="submit" loading={loading}>
              注册
            </Button>
          </Form>
        )}
      </section>
    </main>
  )
}
