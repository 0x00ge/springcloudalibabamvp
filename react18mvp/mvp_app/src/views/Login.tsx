import { useEffect, useMemo, useState } from 'react'
import { Lock, MessageSquare, Phone, User } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router-dom'

import { registerCodeByPhone } from '@/api/apiAuth'
import { useAuth } from '@/store/AuthContext'
import type { CurrentAuthParams } from '@/types/authTypes'
import { notify } from '@/utils/notify'

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

const phonePattern = /^1[3-9]\d{9}$/

const emptyLoginForm: LoginForm = {
  phone: '',
  password: '',
}

const emptyRegisterForm: RegisterForm = {
  phone: '',
  smsCode: '',
  password: '',
  confirmPassword: '',
  name: '',
}

export default function Login() {
  const [authMode, setAuthMode] = useState<AuthMode>('login')
  const [loginForm, setLoginForm] = useState<LoginForm>(emptyLoginForm)
  const [registerForm, setRegisterForm] = useState<RegisterForm>(emptyRegisterForm)
  const [loginErrors, setLoginErrors] = useState<Partial<Record<keyof LoginForm, string>>>({})
  const [registerErrors, setRegisterErrors] = useState<
    Partial<Record<keyof RegisterForm, string>>
  >({})
  const [loading, setLoading] = useState(false)
  const [sendCodeLoading, setSendCodeLoading] = useState(false)
  const [smsCountdown, setSmsCountdown] = useState(0)

  const auth = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  const redirectTarget = useMemo(() => {
    const params = new URLSearchParams(location.search)
    const state = location.state as { redirect?: string } | null

    return params.get('redirect') || state?.redirect || '/home'
  }, [location.search, location.state])

  useEffect(() => {
    if (smsCountdown <= 0) return undefined

    const timer = window.setInterval(() => {
      setSmsCountdown((current) => {
        if (current <= 1) {
          window.clearInterval(timer)
          return 0
        }

        return current - 1
      })
    }, 1000)

    return () => window.clearInterval(timer)
  }, [smsCountdown])

  const validateLogin = () => {
    const errors: Partial<Record<keyof LoginForm, string>> = {}

    if (!loginForm.phone) {
      errors.phone = '请输入手机号'
    } else if (!phonePattern.test(loginForm.phone)) {
      errors.phone = '手机号格式不正确'
    }

    if (!loginForm.password) {
      errors.password = '请输入密码'
    }

    setLoginErrors(errors)
    return Object.keys(errors).length === 0
  }

  const validateRegister = (field?: keyof RegisterForm) => {
    const errors: Partial<Record<keyof RegisterForm, string>> = {}

    if (!registerForm.phone) {
      errors.phone = '请输入手机号'
    } else if (!phonePattern.test(registerForm.phone)) {
      errors.phone = '手机号格式不正确'
    }

    if (!field || field === 'smsCode') {
      if (!registerForm.smsCode) {
        errors.smsCode = '请输入验证码'
      } else if (!/^\d{6}$/.test(registerForm.smsCode)) {
        errors.smsCode = '验证码为 6 位数字'
      }
    }

    if (!field || field === 'password') {
      if (!registerForm.password) {
        errors.password = '请输入密码'
      } else if (registerForm.password.length < 6) {
        errors.password = '密码至少 6 位'
      }
    }

    if (!field || field === 'confirmPassword') {
      if (!registerForm.confirmPassword) {
        errors.confirmPassword = '请再次输入密码'
      } else if (registerForm.confirmPassword.length < 6) {
        errors.confirmPassword = '密码至少 6 位'
      }
    }

    if (!field || field === 'name') {
      if (!registerForm.name) {
        errors.name = '请输入姓名'
      } else if (registerForm.name.length > 50) {
        errors.name = '姓名长度不能超过 50 位'
      }
    }

    if (field) {
      setRegisterErrors((current) => ({
        ...current,
        [field]: errors[field],
      }))
      return !errors[field]
    }

    setRegisterErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleLogin = async () => {
    if (!validateLogin()) return

    setLoading(true)

    try {
      await auth.login(loginForm)
      notify('登录成功', 'success')
      navigate(redirectTarget, { replace: true })
    } finally {
      setLoading(false)
    }
  }

  const handleSendRegisterCode = async () => {
    if (smsCountdown > 0 || !validateRegister('phone')) return

    setSendCodeLoading(true)

    try {
      await registerCodeByPhone({
        phone: registerForm.phone,
      })

      notify('验证码已发送', 'success')
      setSmsCountdown(60)
    } finally {
      setSendCodeLoading(false)
    }
  }

  const handleRegister = async () => {
    if (!validateRegister()) return

    if (registerForm.password !== registerForm.confirmPassword) {
      notify('两次输入的密码不一致', 'error')
      return
    }

    setLoading(true)

    try {
      await auth.register(registerForm as CurrentAuthParams)
      setLoginForm({
        phone: registerForm.phone,
        password: '',
      })
      setAuthMode('login')
      notify('注册成功，请登录', 'success')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = () => {
    if (authMode === 'login') {
      handleLogin()
      return
    }

    handleRegister()
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

        <div className="segmented auth-mode">
          <button
            className={authMode === 'login' ? 'active' : ''}
            onClick={() => setAuthMode('login')}
            type="button"
          >
            登录
          </button>
          <button
            className={authMode === 'register' ? 'active' : ''}
            onClick={() => setAuthMode('register')}
            type="button"
          >
            注册
          </button>
        </div>

        {authMode === 'login' ? (
          <form className="login-form" onSubmit={(event) => event.preventDefault()}>
            <label className="form-field">
              <span className="input-shell">
                <Phone size={18} />
                <input
                  autoComplete="tel"
                  onChange={(event) =>
                    setLoginForm((current) => ({ ...current, phone: event.target.value }))
                  }
                  onKeyDown={(event) => event.key === 'Enter' && handleSubmit()}
                  placeholder="请输入手机号"
                  value={loginForm.phone}
                />
              </span>
              {loginErrors.phone && <span className="field-error">{loginErrors.phone}</span>}
            </label>

            <label className="form-field">
              <span className="input-shell">
                <Lock size={18} />
                <input
                  autoComplete="current-password"
                  onChange={(event) =>
                    setLoginForm((current) => ({ ...current, password: event.target.value }))
                  }
                  onKeyDown={(event) => event.key === 'Enter' && handleSubmit()}
                  placeholder="请输入密码"
                  type="password"
                  value={loginForm.password}
                />
              </span>
              {loginErrors.password && <span className="field-error">{loginErrors.password}</span>}
            </label>

            <button className="button button-primary login-button" disabled={loading} onClick={handleLogin} type="button">
              {loading ? '登录中...' : '登录'}
            </button>
          </form>
        ) : (
          <form className="login-form" onSubmit={(event) => event.preventDefault()}>
            <label className="form-field">
              <span className="input-shell">
                <Phone size={18} />
                <input
                  autoComplete="tel"
                  onChange={(event) =>
                    setRegisterForm((current) => ({ ...current, phone: event.target.value }))
                  }
                  onKeyDown={(event) => event.key === 'Enter' && handleSubmit()}
                  placeholder="请输入手机号"
                  value={registerForm.phone}
                />
              </span>
              {registerErrors.phone && <span className="field-error">{registerErrors.phone}</span>}
            </label>

            <label className="form-field">
              <span className="sms-code-row">
                <span className="input-shell">
                  <MessageSquare size={18} />
                  <input
                    onChange={(event) =>
                      setRegisterForm((current) => ({ ...current, smsCode: event.target.value }))
                    }
                    onKeyDown={(event) => event.key === 'Enter' && handleSubmit()}
                    placeholder="请输入验证码"
                    value={registerForm.smsCode}
                  />
                </span>
                <button
                  className="button sms-code-button"
                  disabled={sendCodeLoading || smsCountdown > 0}
                  onClick={handleSendRegisterCode}
                  type="button"
                >
                  {sendCodeLoading ? '发送中' : smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码'}
                </button>
              </span>
              {registerErrors.smsCode && (
                <span className="field-error">{registerErrors.smsCode}</span>
              )}
            </label>

            <label className="form-field">
              <span className="input-shell">
                <Lock size={18} />
                <input
                  autoComplete="new-password"
                  onChange={(event) =>
                    setRegisterForm((current) => ({ ...current, password: event.target.value }))
                  }
                  onKeyDown={(event) => event.key === 'Enter' && handleSubmit()}
                  placeholder="请输入密码"
                  type="password"
                  value={registerForm.password}
                />
              </span>
              {registerErrors.password && (
                <span className="field-error">{registerErrors.password}</span>
              )}
            </label>

            <label className="form-field">
              <span className="input-shell">
                <Lock size={18} />
                <input
                  autoComplete="new-password"
                  onChange={(event) =>
                    setRegisterForm((current) => ({
                      ...current,
                      confirmPassword: event.target.value,
                    }))
                  }
                  onKeyDown={(event) => event.key === 'Enter' && handleSubmit()}
                  placeholder="请再次输入密码"
                  type="password"
                  value={registerForm.confirmPassword}
                />
              </span>
              {registerErrors.confirmPassword && (
                <span className="field-error">{registerErrors.confirmPassword}</span>
              )}
            </label>

            <label className="form-field">
              <span className="input-shell">
                <User size={18} />
                <input
                  autoComplete="name"
                  onChange={(event) =>
                    setRegisterForm((current) => ({ ...current, name: event.target.value }))
                  }
                  onKeyDown={(event) => event.key === 'Enter' && handleSubmit()}
                  placeholder="请输入姓名"
                  value={registerForm.name}
                />
              </span>
              {registerErrors.name && <span className="field-error">{registerErrors.name}</span>}
            </label>

            <button
              className="button button-primary login-button"
              disabled={loading}
              onClick={handleRegister}
              type="button"
            >
              {loading ? '注册中...' : '注册'}
            </button>
          </form>
        )}
      </section>
    </main>
  )
}
